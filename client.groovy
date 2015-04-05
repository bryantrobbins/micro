@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7.2' )

import groovyx.net.http.RESTClient

def client = new RESTClient('https://localhost:8443/')
client.headers['Authorization'] = 'Basic '+"joe:joespassword".bytes.encodeBase64()

def result = client.get( path : 'greet/any' )
assert result.status == 200
println result.getData()
