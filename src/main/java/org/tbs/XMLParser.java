package org.tbs;

import java.io.File;
import java.io.IOException;
import java.sql.Date;
import java.util.List;
import java.util.ArrayList;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class XMLParser {

    //get list of XML files from the directory
    public List<String> getXmlFiles(String[] directoryPaths) {
        System.out.println("Scanning directory for XML files...");
        List<String> xmlFiles = new ArrayList<>();
        for (String directoryPath : directoryPaths) {
            File directory = new File(directoryPath);
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".xml")) {
                        xmlFiles.add(file.getAbsolutePath());
                    }
                }
            }
        }
        return xmlFiles;
    }
    //parse XML file
    public List<GrantInfo> parseXML(File xmlFile) {
        System.out.println("Parsing XML file: " + xmlFile.getName());

        List<GrantInfo> grants = new ArrayList<>();
        int usGrantCount = 0;
        int foreignGrantCount = 0;
        int totalGrantCount = 0;

        try {
            Document doc = setupDocument(xmlFile);

            //ReturnTypeCd check here
            Node returnTypeNode = doc.getElementsByTagName("ReturnTypeCd").item(0);
            if (returnTypeNode != null) {
                String returnType = returnTypeNode.getTextContent();
                if (!returnType.equals("990PF")) {
                    System.out.println("Skipping file: " + xmlFile.getName() + " because it is not a 990PF");
                    return grants;
                }
            }
            //Filer check here
            Element root = doc.getDocumentElement();
            Element filerElement = (Element) root.getElementsByTagName("Filer").item(0);
            boolean skipApplicationSubmission = true;

            //populate filer information that's common across all grants
            GrantInfo filerInfo = populateFilerInfo(filerElement, doc);
            //populate grant information
            NodeList grantNodes = doc.getElementsByTagName("GrantOrContributionPdDurYrGrp");
            if (grantNodes.getLength() == 0) {
                grantNodes = doc.getElementsByTagName("GrantOrContriPaidDuringYear");
            }
            totalGrantCount = grantNodes.getLength();
            //iterate through each grant node
            for (int i = 0; i < grantNodes.getLength(); i++) {
                Node node = grantNodes.item(i);

                if (isValidNode(node)) {
                    Element grantElement = (Element) node;
                    if (isForeignGrant(grantElement)) {
                        foreignGrantCount++;
                        continue; //skip foreign grants
                    }
                    usGrantCount++;
                    GrantInfo grantInfo = populateGrantInfo(filerInfo, grantElement, doc);
                    grants.add(grantInfo); //add populated grant info to list
                    }
                }
            //print out grant counts
            System.out.println("Total grants: " + totalGrantCount + " From File: " + xmlFile.getName());
            System.out.println("US grants: " + usGrantCount + " From File: " + xmlFile.getName());
            System.out.println("Foreign grants: " + foreignGrantCount + " From File: " + xmlFile.getName());

        } catch (Exception e) {
            e.printStackTrace();
        }
        return grants; //return list of populated grant info
    }
    //setup XML document
    private Document setupDocument(File xmlFile) throws ParserConfigurationException, IOException, SAXException {
        System.out.println("Setting up XML document...");
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        System.out.println("Document setup complete.");
        return dBuilder.parse(xmlFile);
    }
    //check if node is valid
    private boolean isValidNode(Node node) {
        return node != null && node.getNodeType() == Node.ELEMENT_NODE;
    }
    //check if grant is foreign
    private boolean isForeignGrant(Element grantElement) {
        return (grantElement.getElementsByTagName("RecipientUSAddress").getLength() == 0) ||
                (grantElement.getElementsByTagName("RecipientUSAddress").item(0) == null);
    }
    //populate filer information
    private GrantInfo populateFilerInfo(Element filerElement, Document doc) {
        GrantInfo filerInfo = new GrantInfo();
        Element root = doc.getDocumentElement();
        //populate filer address
        Element addressElement = (Element) filerElement.getElementsByTagName("USAddress").item(0);
        if (addressElement != null) {
            Node streetNode = addressElement.getElementsByTagName("AddressLine1Txt").item(0);
            if (streetNode == null) {
                streetNode = addressElement.getElementsByTagName("AddressLine1").item(0);
            }
            Node cityNode = addressElement.getElementsByTagName("City").item(0);
            if (cityNode == null) {
                cityNode = addressElement.getElementsByTagName("CityNm").item(0);
            }
            Node stateNode = addressElement.getElementsByTagName("State").item(0);
            if (stateNode == null) {
                stateNode = addressElement.getElementsByTagName("StateAbbreviationCd").item(0);
            }
            Node zipNode = addressElement.getElementsByTagName("ZIPCd").item(0);
            if (zipNode == null) {
                zipNode = addressElement.getElementsByTagName("ZIPCode").item(0);
            }
            if (streetNode != null) {
                filerInfo.setFilerStreet(streetNode.getTextContent());
            }
            if (cityNode != null) {
                filerInfo.setFilerCity(cityNode.getTextContent());
            }
            if (stateNode != null) {
                filerInfo.setFilerState(stateNode.getTextContent());
            }
            if (zipNode != null) {
                filerInfo.setFilerZip(zipNode.getTextContent());
            }
        }
        //populate filer information fields
        Node taxPeriodEndNode = root.getElementsByTagName("TaxPeriodEndDt").item(0);
        if (taxPeriodEndNode == null) {
            taxPeriodEndNode = root.getElementsByTagName("TaxPeriodEndDate").item(0);
        }
        if (taxPeriodEndNode != null) {
            filerInfo.setTaxPeriodEndDate(Date.valueOf(taxPeriodEndNode.getTextContent()));
        }
        Node returnTypeNode = root.getElementsByTagName("ReturnTypeCd").item(0);
        if (returnTypeNode != null) {
            filerInfo.setReturnTypeCode(root.getElementsByTagName("ReturnTypeCd").item(0).getTextContent());
        }
        Node taxPeriodBeginNode = root.getElementsByTagName("TaxPeriodBeginDt").item(0);
        if (taxPeriodBeginNode == null) {
            taxPeriodBeginNode = root.getElementsByTagName("TaxPeriodBeginDate").item(0);
        }
        if (taxPeriodBeginNode != null) {
            filerInfo.setTaxPeriodBeginDate(Date.valueOf(taxPeriodBeginNode.getTextContent()));
        }
        Node einNode = root.getElementsByTagName("EIN").item(0);
        if (einNode != null) {
            filerInfo.setEin(filerElement.getElementsByTagName("EIN").item(0).getTextContent());
        }
        NodeList businessNameNodes = filerElement.getElementsByTagName("BusinessName");
        if (businessNameNodes.getLength() == 0) {
            businessNameNodes = filerElement.getElementsByTagName("Name");
        }
        if (businessNameNodes.getLength() > 0 && businessNameNodes.item(0) != null) {
            Element businessNameElement = (Element) businessNameNodes.item(0);
            Node businessLineNode = businessNameElement.getElementsByTagName("BusinessNameLine1").item(0);
            if (businessLineNode == null) {
                businessLineNode = businessNameElement.getElementsByTagName("BusinessNameLine1Txt").item(0);
            }
            if (businessLineNode != null) {
                filerInfo.setFilerName(businessLineNode.getTextContent());
            }
        }
        Node totalRevNode = root.getElementsByTagName("TotalRevAndExpnssAmt").item(0);
        if (totalRevNode == null) {
            totalRevNode = root.getElementsByTagName("TotalRevenueAndExpenses").item(0);
        }
        if (totalRevNode != null) {
            String totalRevText = totalRevNode.getTextContent();
            if (totalRevText != null && !totalRevText.isEmpty()) {
                filerInfo.setTotalRev(Long.parseLong(totalRevText));
            }
        }
        Node totalAssetsBOYNode = root.getElementsByTagName("TotalAssetsBOYAmt").item(0);
        if (totalAssetsBOYNode == null) {
            totalAssetsBOYNode = root.getElementsByTagName("TotalAssetsBOY").item(0);
        }
        if (totalAssetsBOYNode != null) {
            String totalAssetsBOYText = totalAssetsBOYNode.getTextContent();
            if (totalAssetsBOYText != null && !totalAssetsBOYText.isEmpty()) {
                filerInfo.setTotalAssetsBOY(Long.parseLong(totalAssetsBOYText));
            }
        }
        Node totalAssetsEOYNode = root.getElementsByTagName("TotalAssetsEOYAmt").item(0);
        if (totalAssetsEOYNode == null) {
            totalAssetsEOYNode = root.getElementsByTagName("TotalAssetsEOY").item(0);
        }
        if (totalAssetsEOYNode != null) {
            String totalAssetsEOYText = totalAssetsEOYNode.getTextContent();
            if (totalAssetsEOYText != null && !totalAssetsEOYText.isEmpty()) {
                filerInfo.setTotalAssetsEOY(Long.parseLong(totalAssetsEOYText));
            }
        }
        Node distributableAmountNode = root.getElementsByTagName("DistributableAmountAsAdjusted").item(0);
        if (distributableAmountNode == null) {
            distributableAmountNode = root.getElementsByTagName("DistributableAmountAsAdjustedAmt").item(0);
        }
        if (distributableAmountNode != null) {
            String distributableAmountText = distributableAmountNode.getTextContent();
            if (distributableAmountText != null && !distributableAmountText.isEmpty()) {
                filerInfo.setDistributableAmount(Long.parseLong(distributableAmountText));
            }
        }
        Node remainingDistributionNode = root.getElementsByTagName("RemainingDistriFromCorpus").item(0);
        if (remainingDistributionNode == null) {
            remainingDistributionNode = root.getElementsByTagName("RemainingDistiFromCorpusAmt").item(0);
        }
        if (remainingDistributionNode != null) {
            String remainingDistributionText = remainingDistributionNode.getTextContent();
            if (remainingDistributionText != null && !remainingDistributionText.isEmpty()) {
                filerInfo.setRemainingDistribution(Long.parseLong(remainingDistributionText));
            }
        }


        //return populated filer information
        return filerInfo;
    }
    //populate grant information
    private GrantInfo populateGrantInfo(GrantInfo filerInfo, Element grantElement, Document doc) {
        GrantInfo grantInfo = new GrantInfo();

        Element addressElement = (Element) grantElement.getElementsByTagName("RecipientUSAddress").item(0);
        if (addressElement != null) {
            Node streetNode = addressElement.getElementsByTagName("AddressLine1Txt").item(0);
            if (streetNode == null) {
                streetNode = addressElement.getElementsByTagName("AddressLine1").item(0);
            }
            Node cityNode = addressElement.getElementsByTagName("City").item(0);
            if (cityNode == null) {
                cityNode = addressElement.getElementsByTagName("CityNm").item(0);
            }
            Node stateNode = addressElement.getElementsByTagName("State").item(0);
            if (stateNode == null) {
                stateNode = addressElement.getElementsByTagName("StateAbbreviationCd").item(0);
            }
            Node zipNode = addressElement.getElementsByTagName("ZIPCd").item(0);
            if (zipNode == null) {
                zipNode = addressElement.getElementsByTagName("ZIPCode").item(0);
            }
            if (streetNode != null) {
                grantInfo.setRecipientStreet(streetNode.getTextContent());
            }
            if (cityNode != null) {
                grantInfo.setRecipientCity(cityNode.getTextContent());
            }
            if (stateNode != null) {
                grantInfo.setRecipientState(stateNode.getTextContent());
            }
            if (zipNode != null) {
                grantInfo.setRecipientZip(zipNode.getTextContent());
            }
        }
        //populate grant information fields
        NodeList businessNameNodes = grantElement.getElementsByTagName("RecipientBusinessName");
        if (businessNameNodes.getLength() == 0) {
            businessNameNodes = grantElement.getElementsByTagName("RecipientPersonNm");
        }
        if (businessNameNodes.getLength() > 0  && businessNameNodes.item(0) != null ) {
            Element businessNameElement = (Element) businessNameNodes.item(0);
            Node businesslineNode = businessNameElement.getElementsByTagName("BusinessNameLine1").item(0);
            if (businesslineNode == null) {
                businesslineNode = businessNameElement.getElementsByTagName("BusinessNameLine1Txt").item(0);
            }
            if (businesslineNode != null) {
                grantInfo.setRecipientName(businesslineNode.getTextContent());
            }
        }
        Node amtNode = grantElement.getElementsByTagName("Amt").item(0);
        if (amtNode == null) {
            amtNode = grantElement.getElementsByTagName("Amount").item(0);
        }
        if (amtNode != null) {
            String amtText = amtNode.getTextContent();
            if (amtText != null && !amtText.isEmpty()) {
                grantInfo.setGrantAmount(Long.parseLong(amtText));
            }
        }
        NodeList foundationStatusNodes = grantElement.getElementsByTagName("RecipientFoundationStatusTxt");
        if (foundationStatusNodes == null) {
            foundationStatusNodes = grantElement.getElementsByTagName("RecipientFoundationStatus");
        }
        if (foundationStatusNodes.getLength() > 0 && foundationStatusNodes.item(0) != null) {
            grantInfo.setRecipientFoundationStatus(foundationStatusNodes.item(0).getTextContent());
        }
        Node purposeNode = grantElement.getElementsByTagName("GrantOrContributionPurposeTxt").item(0);
        if (purposeNode == null) {
            purposeNode = grantElement.getElementsByTagName("GrantOrContributionPurpose").item(0);
        }
        if (purposeNode != null) {
            grantInfo.setGrantPurpose(purposeNode.getTextContent());
        }

        //populate filer information fields
            grantInfo.setTaxPeriodEndDate(filerInfo.getTaxPeriodEndDate());
            grantInfo.setReturnTypeCode(filerInfo.getReturnTypeCode());
            grantInfo.setTaxPeriodBeginDate(filerInfo.getTaxPeriodBeginDate());
            grantInfo.setEin(filerInfo.getEin());
            grantInfo.setFilerName(filerInfo.getFilerName());
            grantInfo.setFilerStreet(filerInfo.getFilerStreet());
            grantInfo.setFilerCity(filerInfo.getFilerCity());
            grantInfo.setFilerState(filerInfo.getFilerState());
            grantInfo.setFilerZip(filerInfo.getFilerZip());
            grantInfo.setTotalRev(filerInfo.getTotalRev());
            grantInfo.setTotalAssetsBOY(filerInfo.getTotalAssetsBOY());
            grantInfo.setTotalAssetsEOY(filerInfo.getTotalAssetsEOY());
            grantInfo.setDistributableAmount(filerInfo.getDistributableAmount());
            grantInfo.setRemainingDistribution(filerInfo.getRemainingDistribution());

//        System.out.println("Grant information populated.");
            return grantInfo;
    }
    //get element text content by tag name from parent element
    private String getElementText(Element parent, String tagName) {
        NodeList list = parent.getElementsByTagName(tagName);
        if (list.getLength() > 0) {
            return list.item(0).getTextContent();
        } else {
            return null;
        }
    }

}
