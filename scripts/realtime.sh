VARS="PublishedLineName
DestinationName
Monitored
AimedArrivalTime
ExpectedArrivalTime
AimedDepartureTime
ExpectedDepartureTime
DeparturePlatformName
StopVisitNote"


echo "/*
 * CODE BLOCK GENERATED BY SCRIPT scripts/realtime.sh
 */"

echo "@Override
public void startElement(String namespaceURI, String localName, 
              String qName, Attributes atts) throws SAXException {"


echo "    if (!inMonitoredStopVisit) {
        if (localName.equals(\"MonitoredStopVisit\")) {
            inMonitoredStopVisit = true;
            data = new RealtimeData();
        }"
for x in $VARS; do
echo "    } else if (localName.equals(\"$x\")) {
        in$x = true;"
done
echo "    }"
echo "}"

echo ""
echo ""

echo "@Override
public void endElement(String namespaceURI, String localName, String qName) {
    if (!inMonitoredStopVisit) return;
    if (localName.equals(\"MonitoredStopVisit\")) {
        /*
         * on StopMatch we're at the end, and we need to add the station to the station list.
         */
        inMonitoredStopVisit = false;
        realtimeList.add(data);"
    
for x in $VARS; do
echo "    } else if (localName.equals(\"$x\")) {
        in$x = false;"
done
echo "    }"
echo "}"


echo ""
echo ""

echo "@Override
public void characters(char ch[], int start, int length) { 
    if (!inMonitoredStopVisit) return;"

for x in $VARS; do
echo "    } else if (in$x) {
        // TODO"
done
echo "    }"
echo "}"

echo "/*
 * END CODE BLOCK GENERATED BY SCRIPT scripts/realtime.sh
 */"
