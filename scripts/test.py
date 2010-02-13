import sys, httplib
import xml.dom.minidom as md
import os



def sendRequest(msg):
    webservice = httplib.HTTP("reis.trafikanten.no")
    webservice.putrequest("POST", "/topp2009/topp2009ws.asmx")
    webservice.putheader("Host", "reis.trafikanten.no")
    #webservice.putheader("User-Agent", "Python post")
    webservice.putheader("Content-type", "text/xml; charset=\"UTF-8\"")
    webservice.putheader("Content-length", "%d" % len(msg))
    #webservice.putheader("SOAPAction", "\"\"")
    webservice.endheaders()
    webservice.send(msg)
    
    statuscode, statusmessage, header = webservice.getreply()
    print "Response: ", statuscode, statusmessage
    print "headers: ", header
    res = webservice.getfile().read()


    #pretty_print = lambda f: '\n'.join([line for line in md.parse(open(f)).toprettyxml(indent=' '*2).split('\n') if line.strip()])
    #str = unicode(webservice.getfile().read(), "utf-8" )

    f = open("test.log","w")
    f.write(res)
    print "test.log saved - ",os.stat("test.log").st_size / 1024,"kb"



def testSearch():
    SoapMessage="""<?xml version="1.0" encoding="utf-8"?>
    <soap12:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:soap12="http://www.w3.org/2003/05/soap-envelope">
        <soap12:Body>
         <GetMatches xmlns="http://www.trafikanten.no/">
           <searchName>varingsko</searchName>
         </GetMatches>
       </soap12:Body>
     </soap12:Envelope>
     """
    sendRequest(SoapMessage)


def testRoute():
    SoapMessage="""<?xml version="1.0" encoding="utf-8"?>
<soap12:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:soap12="http://www.w3.org/2003/05/soap-envelope">
  <soap12:Body>
    <GetTravelsAfter xmlns="http://www.trafikanten.no/">
      <from>3011405</from>
      <to>3012110</to>
      <departureTime>2009-11-22T10:31:00</departureTime>
    </GetTravelsAfter>
  </soap12:Body>
</soap12:Envelope>"""


    
    sendRequest(SoapMessage)
    
def sendAdvancedRoute():
    SoapMessage="""<?xml version="1.0" encoding="utf-8"?>
<soap:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <GetTravelsAdvanced xmlns="http://www.trafikanten.no/">
       <time>2009-12-30T18:38:00</time>
      <from>
        <Stop>
            <ID>3012110</ID>
        </Stop>
      </from>
      <to>
        <Stop>
            <ID>2330160</ID>
        </Stop>
      </to>
      <changemargin>2</changemargin>
      <changepunish>2</changepunish>
      <walkingfactor>100</walkingfactor>
      <proposals>5</proposals>
      <isTravelAfterACertainTimeRatherThanBefore>true</isTravelAfterACertainTimeRatherThanBefore>
 
 
      <transportationtypes/>
      <linenames/>
    </GetTravelsAdvanced>
  </soap:Body>
</soap:Envelope>"""

    
    sendRequest(SoapMessage)
    
    

#testRoute()
#sendAdvancedRoute()
#testSearch()
