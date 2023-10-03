package org.example;

import java.io.File;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.sql.*;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


public class Main {
    public static void main(String[] args) {
        //Initialize DatabaseManager and XMLParser
        DatabaseManager dbManager = new DatabaseManager();
        XMLParser xmlParser = new XMLParser();

        //Get list of XML files from the directory
        List<String> xmlFiles = xmlParser.getXmlFiles("C:\\Users\\truth\\OneDrive\\Desktop\\watershed\\Pf990Db\\pf990\\TY2013");

        //Iterate through each XML file and parse it
        for (String xmlFilePath : xmlFiles) {
            File xmlFileObj = new File(xmlFilePath);
            List<GrantInfo> grants = xmlParser.parseXML(xmlFileObj);

            //Insert each parsed GrantInfo into the database
            for (GrantInfo grant : grants) {
                dbManager.insertGrantInfo(grant);
            }
        }


    }

}































