// Copyright 2016-2019, Pulumi Corporation.  All rights reserved.

import * as aws from "@pulumi/aws";
import * as awsx from "@pulumi/awsx";
import * as pulumi from "@pulumi/pulumi";

const config = new pulumi.Config("bob");
const dbPassword = config.requireSecret("dbPassword");
const queuePassword = config.requireSecret("queuePassword");

const logGroup = new aws.cloudwatch.LogGroup("bob-logs", {
    name: "/ecs/bob",
    retentionInDays: 30,
    tags: { Name: "bob-log-group" }
});

const vpc = new awsx.ec2.Vpc("bob-vpc", {
    enableDnsHostnames: true,
    enableDnsSupport: true,
    cidrBlock: "172.17.0.0/16",
    numberOfAvailabilityZones: 2,
});

// Create a basic cluster and autoscaling group
const cluster = new awsx.ecs.Cluster("bob-cluster", { vpc });
const autoScalingGroup = cluster.createAutoScalingGroup("bob-asg", {
    vpc: vpc,
    subnetIds: vpc.publicSubnetIds,
    templateParameters: {
        minSize: 1,
        maxSize: 5,
        desiredCapacity: 3,
    },
    launchConfigurationArgs: {
        instanceType: "t2.large",
        associatePublicIpAddress: false,
    },
});

const securityGroupIds = cluster.securityGroups.map(g => g.id);

const storageSubnet = new aws.rds.SubnetGroup("bob-sng", {
    subnetIds: vpc.privateSubnetIds,
});

const storage = new aws.rds.Instance("storage", {
    engine: "postgres",

    instanceClass: "db.t2.micro",
    allocatedStorage: 20,

    dbSubnetGroupName: storageSubnet.id,
    vpcSecurityGroupIds: securityGroupIds,

    name: "bob",
    username: "bob",
    password: dbPassword,

    skipFinalSnapshot: true,
});

const bobQueueListener = new awsx.elasticloadbalancingv2.ApplicationListener("bob-queue-listener", {
    port: 5672,
    external: false,
    protocol: "HTTP"
})

const bobQueue = new awsx.ecs.EC2Service("bob-queue", {
    cluster,
    desiredCount: 1,
    taskDefinitionArgs: {
        logGroup,
        containers: {
            "runner": {
                image: "rabbitmq:3-alpine",
                portMappings: [bobQueueListener],
                memory: 128,
            },
        },
    },
});

/* const bobMQ = new aws.mq.Broker("bob-mq", {
    brokerName: "bob-mq",
    engineType: "RabbitMQ",
    engineVersion: "3.8.6",
    hostInstanceType: "mq.t3.micro",
    securityGroups: securityGroupIds,
    subnetIds: vpc.privateSubnetIds,
    users: [{
        username: "bob",
        password: queuePassword,
    }],
    autoMinorVersionUpgrade: true,
});
 */
const environment = [
    { name: "BOB_STORAGE_HOST", value: storage.address },
    { name: "BOB_STORAGE_PASSWORD", value: dbPassword },
    { name: "BOB_QUEUE_HOST", value: bobQueueListener.endpoint.hostname },
    //{ name: "BOB_QUEUE_HOST", value: bobMQ.instances[0].ipAddress }
];

const bobApiserverListener = new awsx.elasticloadbalancingv2.ApplicationListener("bob-apiserver-listener", {
    vpc: vpc,
    external: true,
    port: 7777,
    protocol: "HTTP",
});

const bobApiserver = new awsx.ecs.EC2Service("bob-apiserver", {
    cluster,
    desiredCount: 1,
    taskDefinitionArgs: {
        logGroup,
        containers: {
            "apiserver": {
                image: "ghcr.io/bob-cd/apiserver",
                portMappings: [bobApiserverListener],
                environment: environment,
                memory: 128,
            },
        },
    },
});

const bobEntities = new awsx.ecs.EC2Service("bob-entities", {
    cluster,
    desiredCount: 1,
    taskDefinitionArgs: {
        logGroup,
        containers: {
            "entities": {
                image: "ghcr.io/bob-cd/entities",
                environment: environment,
                memory: 128,
            },
        },
    },
});

const bobRunner = new awsx.ecs.EC2Service("bob-runner", {
    cluster,
    desiredCount: 1,
    taskDefinitionArgs: {
        logGroup,
        containers: {
            "runner": {
                image: "ghcr.io/bob-cd/runner",
                environment: environment,
                memory: 128,
                privileged: true,
            },
        },
    },
});

export const vpcId = vpc.id;
export const vpcPrivateSubnetIds = vpc.privateSubnetIds;
export const vpcPublicSubnetIds = vpc.publicSubnetIds;
export const bobEndpoint = bobApiserverListener.endpoint.hostname;
export const queueEndpoint = bobQueueListener.endpoint.hostname;
