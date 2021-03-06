import boto3
from elasticsearch import Elasticsearch, RequestsHttpConnection
from requests_aws4auth import AWS4Auth

region = 'us-east-2'
service = 'es'
credentials = boto3.Session().get_credentials()
awsauth = AWS4Auth(credentials.access_key, credentials.secret_key, region, service, session_token=credentials.token)

host = 'search-bigdata-analytics-h2gqq7kopvhhi5rjzjpad6xe7e.us-east-2.es.amazonaws.com'

es = Elasticsearch(
        hosts=[{'host': host, 'port': 443}],
        http_auth=awsauth,
        use_ssl=True,
        verify_certs=True,
        connection_class=RequestsHttpConnection
        )

def lambda_handler(event, context):
    for record in event['Records']:
        ddbARN = record['eventSourceARN']
        ddbTable = ddbARN.split(':')[5].split('/')[1]
        
        if ddbTable == "users_states_log":
            index = "user-states"
            es.index(index=index, doc_type='state', id=record['dynamodb']['Keys']['username']['S'] + str(record['dynamodb']['Keys']['timestamp']['N']), body=record["dynamodb"])
        elif ddbTable == "sensor_data":
            index = "raw-sensor-data"
            es.index(index=index, doc_type='raw-sensor-data', id=record['dynamodb']['Keys']['compositeKey']['S'], body=record["dynamodb"])

