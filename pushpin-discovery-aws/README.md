# Pushpin AWS Discovery Module

This module provides AWS-based discovery for Pushpin servers running on EC2 instances. It supports discovering instances by tags and from Auto Scaling Groups.

## Usage

To use this module, add it as a dependency to your project:

```kotlin
// Gradle (Kotlin DSL)
implementation("io.github.mpecan:pushpin-missing-toolbox-discovery-aws:0.0.1-SNAPSHOT")
```

The module uses Spring Boot's auto-configuration to automatically set up AWS discovery when:
1. The AWS SDK EC2 client is on the classpath
2. The `pushpin.discovery.aws.enabled` property is set to `true`

## Configuration

The AWS discovery mechanism can be configured using the following properties:

```properties
# Enable AWS discovery (disabled by default)
pushpin.discovery.aws.enabled=true

# AWS region to use
pushpin.discovery.aws.region=us-east-1

# Optional custom endpoint for testing with localstack
# pushpin.discovery.aws.endpoint=http://localhost:4566

# Tags to filter EC2 instances by (key-value pairs)
pushpin.discovery.aws.tags.service=pushpin
pushpin.discovery.aws.tags.environment=production

# Auto Scaling Group configuration
pushpin.discovery.aws.useAutoScalingGroups=true
# Optional: filter by specific ASG names
pushpin.discovery.aws.autoScalingGroupNames[0]=pushpin-asg-prod
pushpin.discovery.aws.autoScalingGroupNames[1]=pushpin-asg-backup

# Optional role to assume for cross-account access
# pushpin.discovery.aws.assumeRoleArn=arn:aws:iam::123456789012:role/PushpinDiscoveryRole

# Cache refresh interval in minutes
pushpin.discovery.aws.refreshCacheMinutes=5

# Whether to check EC2 instance health status
pushpin.discovery.aws.instanceHealthCheckEnabled=true

# Default ports for discovered Pushpin servers (if not specified by instance tags)
pushpin.discovery.aws.port=7999
pushpin.discovery.aws.controlPort=5564
pushpin.discovery.aws.publishPort=5560

# Health check path
pushpin.discovery.aws.healthCheckPath=/api/health/check

# Use private IP addresses (true) or public (false)
pushpin.discovery.aws.privateIp=true
```

## Customization

This module is designed to be customizable. You can provide your own implementations of the following interfaces:

1. `Ec2InstancesProvider` - for customizing how EC2 instances are discovered
2. `InstanceHealthChecker` - for customizing how instance health is checked
3. `InstanceConverter` - for customizing how EC2 instances are converted to PushpinServer objects

To provide your own implementation, simply define a bean in your Spring application:

```kotlin
@Configuration
class CustomAwsDiscoveryConfig {

    @Bean
    fun ec2InstancesProvider(): Ec2InstancesProvider {
        return CustomEc2InstancesProvider()
    }
    
    @Bean
    fun instanceHealthChecker(): InstanceHealthChecker {
        return CustomInstanceHealthChecker()
    }
    
    @Bean
    fun instanceConverter(): InstanceConverter {
        return CustomInstanceConverter()
    }
}
```

## Required AWS Permissions

The AWS discovery mechanism requires the following IAM permissions:

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "ec2:DescribeInstances",
                "ec2:DescribeInstanceStatus"
            ],
            "Resource": "*"
        },
        {
            "Effect": "Allow",
            "Action": [
                "autoscaling:DescribeAutoScalingGroups"
            ],
            "Resource": "*"
        }
    ]
}
```

### Using IAM Roles

For EC2 instances, the simplest way to provide these permissions is to attach an IAM role with the necessary permissions to the instances running your application.

If you need to discover Pushpin servers across AWS accounts, you can:

1. Create a role in the target account with the necessary permissions
2. Configure the `pushpin.discovery.aws.assumeRoleArn` property with the ARN of the role
3. Ensure the role has a trust relationship allowing your application to assume it

## Cost Considerations

The AWS discovery mechanism makes API calls to AWS services which may incur costs:

- EC2 DescribeInstances and DescribeInstanceStatus API calls
- AutoScaling DescribeAutoScalingGroups API calls

To minimize costs:

1. Consider increasing the cache refresh interval (`pushpin.discovery.aws.refreshCacheMinutes`)
2. Use specific tags and/or ASG names to limit the scope of API calls
3. Set `pushpin.discovery.aws.instanceHealthCheckEnabled=false` if health checks are not required

## EC2 Instance Requirements

EC2 instances running Pushpin servers should:

1. Have appropriate tags matching your configuration (e.g., `service=pushpin`)
2. Be in the 'running' state
3. Have health status 'OK' if health checks are enabled
4. Be accessible from your application (network, security groups, etc.)

## Example Setup

### 1. Tag EC2 Instances

Ensure your Pushpin EC2 instances have appropriate tags:

```
Name: pushpin-server-1
service: pushpin
environment: production
```

### 2. Configure Discovery

In your `application.properties` or `application.yml`:

```properties
# Enable Pushpin discovery
pushpin.discovery.enabled=true

# Configure AWS discovery
pushpin.discovery.aws.enabled=true
pushpin.discovery.aws.region=us-east-1
pushpin.discovery.aws.tags.service=pushpin
pushpin.discovery.aws.tags.environment=production
pushpin.discovery.aws.refreshCacheMinutes=5
pushpin.discovery.aws.privateIp=true
```

### 3. Advanced: Setting Up Cross-Account Discovery

If you need to discover Pushpin servers in another AWS account:

#### In the target account:

1. Create an IAM role with appropriate permissions (see above)
2. Add a trust relationship allowing your application's account to assume the role

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "AWS": "arn:aws:iam::YOUR_ACCOUNT_ID:role/YourApplicationRole"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}
```

#### In your application configuration:

```properties
pushpin.discovery.aws.assumeRoleArn=arn:aws:iam::TARGET_ACCOUNT_ID:role/PushpinDiscoveryRole
```

## Troubleshooting

If you're having issues with AWS discovery:

1. Check that AWS discovery is enabled (`pushpin.discovery.aws.enabled=true`)
2. Verify your AWS credentials and region configuration
3. Ensure EC2 instances have the expected tags
4. Check that instances are in the 'running' state and passing health checks
5. Verify network connectivity between your application and Pushpin servers
6. Check logs for error messages (set logging level to DEBUG for more details)
   ```properties
   logging.level.io.github.mpecan.pmt.discovery.aws=DEBUG
   ```
7. For testing, consider using a local endpoint with LocalStack:
   ```properties
   pushpin.discovery.aws.endpoint=http://localhost:4566
   ```