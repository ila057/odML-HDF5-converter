import core.MetadataParser;
import odml.core.Reader;
import org.g_node.nix.Block;
import org.g_node.nix.Property;
import org.g_node.nix.Section;
import org.g_node.nix.Value;
import org.apache.log4j.*;


import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Vector;


/**
 * Created by ipsita on 23/6/16.
 */
public class MetadataParserImpl implements MetadataParser {

    String metadataFile;
    final static Logger logger = Logger.getLogger(MetadataParserImpl.class);

    public MetadataParserImpl(String metadatFile) {
        this.metadataFile = metadatFile;
    }

    public MetadataParserImpl() {
        this.metadataFile = "metadata.xml";
    }


    public odml.core.Section initializeODMLReader(){
        Reader reader = new Reader();
        odml.core.Section rootSection=new odml.core.Section();
        InputStream inputstream;
        try {
            inputstream = new FileInputStream(metadataFile);
            rootSection = reader.load(inputstream, true);
        } catch (Exception e) {
            logger.error("Exception occurred while loading  and reading odML input file. Please check if correct metadata file is provided "+e);
            throw new RuntimeException("context",e);
        }
        return rootSection;
    }

    /**
     *
     * This method takes in a nix file, a nix Block and a odml metadata file, parses the odml
     * metadata file and creates a metadata for the Block in the Nix file.
     *
     * @param metadataFile : File name of the metadata.xml odml file
     * @param block : Block whose metadata will be set
     * @param file : HDF5 (Specifically nix) file in which this metadata will be written
     */
    public void setMetadata(String metadataFile, Block block, org.g_node.nix.File file) {

        java.io.File inputFile = new java.io.File(metadataFile);

        try {
            odml.core.Section rootSection = initializeODMLReader();
            String version = rootSection.getDocumentVersion();
            String date = rootSection.getDocumentDate().toString();

            logger.info("version : " + version);
            logger.info("date : " + date);

            // create section and add a property
            Section rootSectionMetadata = file.createSection("metadataSection", "metadata");
            block.setMetadata(rootSectionMetadata);

            //for parsing
            Vector<odml.core.Section> sectionVector = rootSection.getSections();

            if(sectionVector.size()>0){
                setSection(rootSectionMetadata, sectionVector);
            }
        } catch (Exception e) {
            logger.error("Exception occurred while setting metadata in hdf5 file "+e);
            throw new RuntimeException("context",e);
        }

    }

    public void setSection(Section parentSection, Vector<odml.core.Section> sectionVector){
        for (int temp = 0; temp < sectionVector.size(); temp++)
        {

            odml.core.Section thisSection = sectionVector.get(temp);
            logger.debug("temp : " + temp + " | Current section : " + thisSection.getName());

            String typeOfSection = "";
            String nameOfSection = "";
            Vector<odml.core.Property> propertiesList = new Vector<>();

            if(thisSection.getType()!=null){
                typeOfSection = thisSection.getType();
            }

            if(thisSection.getName()!=null){
                nameOfSection = thisSection.getName();
            }
            // hdf5 creating subsection of metadata section (root)
            Section secChild = parentSection.createSection(nameOfSection, typeOfSection);

            //System.out.println("This section's sections: "+ thisSection.getSections().size());

            //this is the recursive function since a section may have a section inside it, and so on.
            if(thisSection.getSections()!=null && thisSection.getSections().size()>0){
                setSection(secChild, thisSection.getSections());
            }

            if(thisSection.getProperties()!=null){
                propertiesList = thisSection.getProperties();
            }
            setProperties(secChild, propertiesList);
        }


    }

    public void setProperties(Section parentSec, Vector<odml.core.Property> propertiesList){
        //for each child of section (for property)
        if(propertiesList!=null && !propertiesList.isEmpty()){
            String nameOfProperty = "";
            Value valueOfProperty = new Value("");
            for(int tempProperty = 0; tempProperty < propertiesList.size(); tempProperty++) {
                odml.core.Property thisProperty = propertiesList.get(tempProperty);
                logger.debug("    tempProperty : " + tempProperty + " | Current property : " + thisProperty.getName());
                if(thisProperty.getName()!=null){
                    nameOfProperty = thisProperty.getName();
                }


                //System.out.println("-----------type: "+ thisProperty.getWholeValue().getMap().get("type") + " | value " + thisProperty.getValue());
                if(thisProperty.valueCount()>0){
                    odml.core.Value wholeValue = thisProperty.getWholeValue();
                    String value = thisProperty.getValue().toString();
                    String valueType= wholeValue.getMap().get("type").toString();
                    switch (valueType) {
                        case "datetime":
                            valueOfProperty.setString(value);
                            break;
                        case "int":
                            valueOfProperty.setInt(Integer.parseInt(value));
                            break;
                        case "float":
                            valueOfProperty.setDouble(Double.parseDouble(value));
                            break;
                        case "boolean":
                            valueOfProperty.setBoolean(Boolean.parseBoolean(value));
                            break;
                        case "string":
                            valueOfProperty.setString(value);
                            break;
                        case "long":
                            valueOfProperty.setLong(Long.parseLong(value));
                            break;
                        default:
                            logger.error("ERROR. Some wrong valueType. valueType : " + valueType);
                    }
                }
                processGUINamespaces(parentSec, thisProperty);
                Property prop = parentSec.createProperty(nameOfProperty, valueOfProperty);
            }

        }
    }

//    public void setGUINamespace(String nameOfGUINamespace, Boolean valueOfGUINamespace, Section guiSection){
//        Value gui_required_value = new Value(valueOfGUINamespace);
//        guiSection.createProperty(nameOfGUINamespace, gui_required_value);
//    }

    public void processGUINamespaces(Section parentSec, odml.core.Property property){
        Section guiSection = parentSec.createSection(property.getName(), "GUI:Namespace");

        List list = property.getGuiHelper().getGUINamespaceTags();
        for(int i=0; i<list.size(); i++){
            org.jdom.Element guiElement = (org.jdom.Element) list.get(i);
            String elementName = new String("gui_"+guiElement.getName());
            String elementValue = guiElement.getValue();
            logger.debug("->"+list.get(i));

            Value value = new Value(elementValue);
            guiSection.createProperty(elementName, value);

        }
    }

}