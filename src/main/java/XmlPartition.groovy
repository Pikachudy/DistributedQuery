import javax.xml.stream.*
pieces = 24 // 切分块数
input = "D:\\homework\\Distribution\\dblp\\dblp.xml" // xml源文件路径
output = "output_%04d.xml" // 输出命名
eventFactory = XMLEventFactory.newInstance()
fileNumber = elementCount = 0
System.setProperty("entityExpansionLimit","3000000")
def createEventReader() {
    reader = XMLInputFactory.newInstance().createXMLEventReader(new FileInputStream(input))
    while(true){
        try{
            start = reader.next()
        }catch(XMLStreamException e){
            //println e
            continue
        }
//        if(start == XMLStreamConstants.DTD){
//            continue
//        }
        break
    }
    while(true){
        try{
            root = reader.nextTag()
        }catch(XMLStreamException e){
            //println e
            continue
        }
        break
    }
    while (true){
        try{
            firstChild = reader.nextTag()
        }catch(XMLStreamException e){
            //println e
            continue
        }
        break
    }
    return reader
}
def createNextEventWriter () {
    println "Writing to '${filename = String.format(output, ++fileNumber)}'"
    writer = XMLOutputFactory.newInstance().createXMLEventWriter(new FileOutputStream(filename), start.characterEncodingScheme)
    writer.add(start)
    writer.add(root)
    return writer
}
elements = createEventReader().findAll { it.startElement && it.name == firstChild.name }.size()
println "Splitting ${elements} <${firstChild.name.localPart}> elements into ${pieces} pieces"
chunkSize = elements/pieces
writer = createNextEventWriter()
writer.add(firstChild)
createEventReader().each {
    if (it.startElement && it.name == firstChild.name) {
        if (++elementCount > chunkSize) {
            writer.add(eventFactory.createEndDocument())
            writer.flush()
            writer = createNextEventWriter()
            elementCount = 0
        }
    }
    writer.add(it)
}
writer.flush()