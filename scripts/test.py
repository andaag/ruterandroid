import sys, httplib
import xml.dom.minidom as md

SoapMessage="""
<soap12:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:soap12="http://www.w3.org/2003/05/soap-envelope">
 <soap12:Body>
   <GetTravelsAfter xmlns="http://www.trafikanten.no/">
    <from>3012110</from>
    <to>3012121</to>
    <departureTime>2009-11-05T21:35:00</departureTime>
   </GetTravelsAfter>
  </soap12:Body>
</soap12:Envelope>
"""


print SoapMessage

#construct and send the header

webservice = httplib.HTTP("reis.trafikanten.no")
webservice.putrequest("POST", "/topp2009/topp2009ws.asmx")
webservice.putheader("Host", "reis.trafikanten.no")
#webservice.putheader("User-Agent", "Python post")
webservice.putheader("Content-type", "text/xml; charset=\"UTF-8\"")
webservice.putheader("Content-length", "%d" % len(SoapMessage))
#webservice.putheader("SOAPAction", "\"\"")
webservice.endheaders()
webservice.send(SoapMessage)

# get the response

statuscode, statusmessage, header = webservice.getreply()
print "Response: ", statuscode, statusmessage
print "headers: ", header
res = webservice.getfile().read()


#pretty_print = lambda f: '\n'.join([line for line in md.parse(open(f)).toprettyxml(indent=' '*2).split('\n') if line.strip()])
#str = unicode(webservice.getfile().read(), "utf-8" )

f = open("test.log","w")
f.write(res)


