# AWS Monitoring
This cartridge is use for collecting the AWS resources from all regions and send them as report in attachment.

## AWS resources
The following resources are monitored as part of this cartridge:

 * EC2
 * VPC
 * Elastic IP
 * KeyPair
 * S3 buckets
 * IAM Users and Groups

## User Inputs
This Jenkins job requires three user inputs:

* EMAIL_RECIPIENTS
  * This is a comma separated list (optional whitespace after commas) of email addresses that should receive AWS report.
* AWS_AccountName
  * AWS account name for example DCSC-training
* AWS_CREDENTIALS
  * AWS access key and secret key for your account

## Schedule build
This Jenkins job is scheduled to build weekly at 12:00 AM on every Sunday. This schedule can be modified in job configuration.

## E-Mail Notification
E-mail would be send after successful completion of the Jenkins Job. Jenkins configuration for sending e-mail should be done as prerequisite for sending emails.
