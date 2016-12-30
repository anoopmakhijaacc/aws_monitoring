// Folders
def workspaceFolderName = "${WORKSPACE_NAME}"
def projectFolderName = "${PROJECT_NAME}"

//Label for nodes to restrict build to:
def jenkinsNodeLabel = "aws"

//Id for aws user/pass credentials in jobs
def awsString = "aws"

def awsmonitorJobName = "AWS_Resource_Monitoring"
def awsmonitorJob = freeStyleJob(projectFolderName + "/" + awsmonitorJobName)

// Create Environment
awsmonitorJob.with{
  description('''This job will monitor AWS resources in all regions.''')
  logRotator(10, 20)
  parameters {
    stringParam('EMAIL_RECIPIENTS', '', 'This is a comma separated list (optional whitespace after commas) of email addresses that should receive aws report. May contain references to build parameters. To CC or BCC someone instead of putting them in the To list, add cc: or bcc: before the email address (e.g., cc:someone@example.com, bcc:bob@example.com).')
    stringParam('AWS_AccountName', '', 'AWS account name')
    credentialsParam("AWS_CREDENTIALS"){
			type('com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl')
			defaultValue('aws')
			description('AWS access key and secret key for your account')
		}
  }
  triggers {
        cron('0 0 * * 7')
    }
  label(jenkinsNodeLabel)
  environmentVariables {
      env('WORKSPACE_NAME',workspaceFolderName)
      env('PROJECT_NAME',projectFolderName)
  }
  wrappers {
      preBuildCleanup()
      injectPasswords()
      credentialsBinding {
          usernamePassword("AWS_ACCESS_KEY_ID", "AWS_SECRET_ACCESS_KEY", '${AWS_CREDENTIALS}')
      }
  }

  steps {
      shell('''#!/bin/bash
rm -rf ./aws_resource/
mkdir aws_resource
for region in us-east-1 us-east-2 us-west-1 us-west-2 ca-central-1 ap-south-1 ap-northeast-2 ap-southeast-1 ap-southeast-2 ap-northeast-1 eu-central-1 eu-west-1 eu-west-2 sa-east-1; do

###############################################################################################################
#Check for EC2
echo "Querying EC2 in $region...."
echo -e "\n######################### AWS REGION $region ###########################\n\n" >> aws_resource/EC2_Instances.txt
aws ec2 describe-instances --region $region --query 'Reservations[*].Instances[*].{Instance_ID:InstanceId,State:State.Name,Instance_Type:InstanceType,keyName:KeyName,Instance_Name:Tags[?Key==`Name`].Value | [0]}' --output table >> aws_resource/EC2_Instances.txt
#################################################################################################################

###############################################################################################################
#Check for VPC
echo "Querying VPC in $region...."
echo -e "\n######################### AWS REGION $region ###########################\n\n" >> aws_resource/VPC.txt
aws ec2 describe-vpcs --region $region --query 'Vpcs[*].{VpcId:VpcId,CidrBlock:CidrBlock,State:State,InstanceTenancy:InstanceTenancy,IsDefault:IsDefault}' --output table >> aws_resource/VPC.txt
#################################################################################################################

###############################################################################################################
#Check for EIP
echo "Querying EIP in $region...."
echo -e "\n######################### AWS REGION $region ###########################\n\n" >> aws_resource/EIP.txt
aws ec2 describe-addresses --region $region --query 'Addresses[*].{AllocationId:AllocationId,EIP:PublicIp,InstanceId:InstanceId,Instance_PrivateIpAddress:PrivateIpAddress}' --output table >> aws_resource/EIP.txt
#################################################################################################################

###############################################################################################################
#Check for KeyPair
echo "Querying KeyPair in $region...."
echo -e "\n######################### AWS REGION $region ###########################\n\n" >> aws_resource/KeyPair.txt
aws ec2 describe-key-pairs --region $region --output table >> aws_resource/KeyPair.txt
#################################################################################################################

done

###############################################################################################################
#Check for S3 bucket
echo "Querying S3 buckets...."
echo -e "List of S3 buckets:\n" >> aws_resource/S3.txt
aws s3 ls | cut -f3 -d" " >> aws_resource/S3.txt
#################################################################################################################

###############################################################################################################
#Check for IAM User
echo "Querying IAM User...."
echo -e "List of IAM Users:\n" >> aws_resource/IAM.txt
aws iam list-users --query 'Users[*].{IAM_User:UserName,User_CreateDate:CreateDate,User_LastLoginOn:PasswordLastUsed}' --output table >> aws_resource/IAM.txt

echo -e "\n\nList of IAM Groups:\n" >> aws_resource/IAM.txt
aws iam list-groups --query 'Groups[*].{GroupName:GroupName,Group_CreateDate:CreateDate}' --output table >> aws_resource/IAM.txt
#################################################################################################################

#Delete Empty files
for file in aws_resource/*.txt; do
        if [ ! -s $file ]; then
                rm -f $file
        fi
done
'''.stripMargin())
  }
  publishers {
    extendedEmail {
      recipientList('''$EMAIL_RECIPIENTS''')
      replyToList('''$DEFAULT_REPLYTO''')
      attachmentPatterns('aws_resource/*.txt')
      defaultSubject('''List of AWS Resources on $AWS_AccountName AWS account''')
      defaultContent('''Hi,

Please find attached the txt files containing details for all existing aws resources in each region for the $AWS_AccountName.

Thank you,
DCSC Team
''')
      preSendScript('''$DEFAULT_PRESEND_SCRIPT''')
      triggers {
        success{}
      }
    }
    wsCleanup {
          setFailBuild(false)
          cleanWhenAborted(false)
          cleanWhenFailure(false)
          cleanWhenNotBuilt(false)
          cleanWhenUnstable(false)
        }
  }
}
