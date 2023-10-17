package com.mto.ca;
//import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
//import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
//import java.util.List;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.security.auth.callback.CallbackHandler;
//import org.apache.xpath.XPathAPI;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import com.ibm.wsspi.security.auth.callback.Constants;
import com.ibm.wsspi.security.auth.callback.WSMappingCallbackHandlerFactory;
import com.ibm.websphere.bo.BOFactory;
import com.ibm.websphere.http.data.bindings.HTTPStreamDataBinding;
//import com.ibm.websphere.http.data.bindings.HTTPStreamDataBindingXML;
import com.ibm.websphere.http.data.streams.HTTPInputStream;
import com.ibm.websphere.http.data.streams.HTTPOutputStream;
import com.ibm.websphere.http.headers.HTTPControl;
import com.ibm.websphere.http.headers.HTTPHeaders;
import com.ibm.websphere.sca.ServiceManager;
import commonj.connector.runtime.DataBindingException;
import commonj.sdo.DataObject;
//import commonj.sdo.Property;
import javax.security.auth.login.LoginContext;
import javax.security.auth.Subject;
import java.util.Set;
import javax.resource.spi.security.PasswordCredential;
//@SuppressWarnings("unchecked")
public class NativeDataBinding implements HTTPStreamDataBinding 
{

	private DataObject fieldDataObject;
	private HTTPControl httpControl;
	private HTTPHeaders httpHeaders;
	private boolean isBusinessException = false;
	private transient ByteArrayOutputStream nativeData;
	private String certificatePrefix;
	
	public NativeDataBinding() {
		nativeData = new ByteArrayOutputStream();
	}

	public void convertToNativeData() throws DataBindingException {
		DataObject dataObject = getDataObject();
		System.out.println("Data object on request :" + dataObject.toString());
		
		try {
			String completeCertificateNumber=URLEncoder.encode(dataObject.getString("certNum"), "UTF-8");
			//String completeCertificateNumber="309B-123456";
			String certificateNumber = completeCertificateNumber.substring(completeCertificateNumber.lastIndexOf("-")+1);
			certificatePrefix = completeCertificateNumber.substring(0, completeCertificateNumber.lastIndexOf("-"));
			HTTPControl controlParameters = getControlParameters();
			System.out.println(controlParameters.getDynamicOverrideURL() + "/"+certificateNumber+"/");
			System.out.println("URL Value is" + controlParameters.getDynamicOverrideURL());			
			controlParameters.setDynamicOverrideURL(controlParameters.getDynamicOverrideURL()+ "/"+certificateNumber+"/");
			//controlParameters.setDynamicOverrideURL(controlParameters.getDynamicOverrideURL());
		} catch (Exception e) {
			e.printStackTrace(System.out);
		}		
	}
	
	public void convertFromNativeData(HTTPInputStream arg0) throws DataBindingException, IOException {
		ServiceManager serviceManager = ServiceManager.INSTANCE;
		BOFactory bofactory = (BOFactory) serviceManager.locateService("com/ibm/websphere/bo/BOFactory");
		DataObject tradeObject = bofactory.create("http://Lib_OCOT", "Trade");
		
		nativeData.reset();
		byte[] buf = new byte[1024];
		int read = -1;
		while((read = arg0.read(buf, 0, 1024)) != -1) {
			nativeData.write(buf,0, read);
		}	
		String response=new String(nativeData.toByteArray(),"utf-8");
		System.out.println("response is "+response );
		String strCipherText = response;
		//String strCipherText=response.substring(response.indexOf("<output1>")+9, response.indexOf("</output1>"));
		System.out.println("String to be decrypted : "+ strCipherText);
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(false);
		try {
			Map map=new HashMap();
			map.put(Constants.MAPPING_ALIAS, "OCOT_Key");
			CallbackHandler callbackHandler = WSMappingCallbackHandlerFactory.getInstance().getCallbackHandler(map, null);
			LoginContext loginContext = new LoginContext("DefaultPrincipalMapping", callbackHandler);
			loginContext.login();
			Subject subject = loginContext.getSubject();
			Set credentials = subject.getPrivateCredentials();
			PasswordCredential passwordCredential = (PasswordCredential) credentials.iterator().next();
			String ekey = new String(passwordCredential.getPassword());
			System.out.println("ekey is : "+ekey);
			SecretKeySpec secretKey = new SecretKeySpec(ekey.getBytes("UTF-8"), "AES");
			String strDecryptedText="";
			Cipher aesCipherForDecryption = Cipher.getInstance("AES/CBC/PKCS5PADDING"); // Must specify the mode explicitly as most JCE providers default to ECB mode!! 
			byte[] byteivAndDecryptData = DatatypeConverter.parseBase64Binary(strCipherText); 
			byte[] byteDecryptIv = Arrays.copyOfRange(byteivAndDecryptData, 0, 16);
			byte[] byteEncryptedText = Arrays.copyOfRange(byteivAndDecryptData, 16, byteivAndDecryptData.length); 
			aesCipherForDecryption.init(Cipher.DECRYPT_MODE, secretKey,
			new IvParameterSpec(byteDecryptIv));
			byte[] byteDecryptedText = aesCipherForDecryption.doFinal(byteEncryptedText);
			strDecryptedText = new String(byteDecryptedText);
			//strDecryptedText="<a><Trade><ReferenceFile>000331519</ReferenceFile><FolderRSN>800130</FolderRSN><FolderType>310J</FolderType><FolderDesc>Truck Trailer Service Technician</FolderDesc>                                 <SubDesc>Journeypersons Class</SubDesc><RecognitionType>Certificate of Qualification</RecognitionType><StatusDesc>Active</StatusDesc><IssueDate>2013-12-06T09:53:32.743</IssueDate><Exams /></Trade><Trade><ReferenceFile>000331520</ReferenceFile>                                 <FolderRSN>800130</FolderRSN>                                 <FolderType>310S</FolderType>                                 <FolderDesc>Truck Trailer Service Technician</FolderDesc>                                 <SubDesc>Journeypersons Class</SubDesc>                                 <RecognitionType>Certificate of Qualification</RecognitionType>                                 <StatusDesc>Active</StatusDesc>                                 <IssueDate>2013-12-06T09:53:32.743</IssueDate>                                 <Exams />                 </Trade></a>";
			System.out.println(" Decrypted Text message is:" + strDecryptedText);
			System.out.println("INFO:OCOT:Certificate prefix:"+ certificatePrefix);			
			Document rdf = factory.newDocumentBuilder().parse(new InputSource(new StringReader(strDecryptedText)));
			NodeList trade = rdf.getElementsByTagName("Trade");
			Element tradeElement=(Element) trade.item(0);
			boolean isDataFound = false;
			for(int i=0; i<trade.getLength();i++){
				Element tradeElementTemp=(Element) trade.item(i);
				NodeList FolderTypeNodeList = tradeElementTemp.getElementsByTagName("FolderType");
				System.out.println("INFO:Inside For: foldertype:" + FolderTypeNodeList.item(0).getTextContent());
				if(FolderTypeNodeList.getLength()>0 && FolderTypeNodeList.item(0).getTextContent().equals(certificatePrefix) ){
					System.out.println("INFO:Inside IF: foldertype:" + FolderTypeNodeList.item(0).getTextContent());
					tradeElement=tradeElementTemp;
					isDataFound = true;
					break;					
				}
			}	
			if(tradeElement != null && isDataFound){
				NodeList referenceFileNodeList = tradeElement.getElementsByTagName("ReferenceFile");
				if(referenceFileNodeList.getLength()>0){
					System.out.println("Reference File Value is : "+referenceFileNodeList.item(0).getTextContent());
					tradeObject.setString("ReferenceFile", referenceFileNodeList.item(0).getTextContent());
				}
				else{
					tradeObject.setString("ReferenceFile", "");
				}
				
				NodeList folderRSNNodeList = tradeElement.getElementsByTagName("FolderRSN");
				if(folderRSNNodeList.getLength()>0){
					tradeObject.setString("FolderRSN", folderRSNNodeList.item(0).getTextContent());
				}
				else{
					tradeObject.setString("FolderRSN", "");
				}
				
				NodeList folderTypeNodeList = tradeElement.getElementsByTagName("FolderType");
				if(folderTypeNodeList.getLength()>0){
					tradeObject.setString("FolderType", folderTypeNodeList.item(0).getTextContent());
				}
				else{
					tradeObject.setString("FolderType", "");
				}
				
				NodeList folderDescNodeList = tradeElement.getElementsByTagName("FolderDesc");
				if(folderDescNodeList.getLength()>0){
					tradeObject.setString("FolderDesc", folderDescNodeList.item(0).getTextContent());
				}
				else{
					tradeObject.setString("FolderDesc", "");
				}
				
				NodeList subDescNodeList = tradeElement.getElementsByTagName("SubDesc");
				if(subDescNodeList.getLength()>0){
					tradeObject.setString("SubDesc", subDescNodeList.item(0).getTextContent());
				}
				else{
					tradeObject.setString("SubDesc", "");
				}
				
				NodeList recognitionTypeNodeList = tradeElement.getElementsByTagName("RecognitionType");
				if(recognitionTypeNodeList.getLength()>0){
					tradeObject.setString("RecognitionType", recognitionTypeNodeList.item(0).getTextContent());
				}
				else{
					tradeObject.setString("RecognitionType", "");
				}
				
				NodeList statusDescNodeList = tradeElement.getElementsByTagName("StatusDesc");
				if(statusDescNodeList.getLength()>0){
					tradeObject.setString("StatusDesc", statusDescNodeList.item(0).getTextContent());
				}
				else{
					tradeObject.setString("StatusDesc", "");
				}
				
				NodeList issueDateNodeList = tradeElement.getElementsByTagName("IssueDate");
				if(issueDateNodeList.getLength()>0){
					tradeObject.set("IssueDate", issueDateNodeList.item(0).getTextContent());
				}
				
				
				NodeList expiryDateNodeList = tradeElement.getElementsByTagName("ExpiryDate");
				System.out.println("Near ExpiryDate");
				if(expiryDateNodeList.getLength()>0){
					System.out.println("Expiry Date : "+expiryDateNodeList.item(0).getTextContent());
					String expiryDate=expiryDateNodeList.item(0).getTextContent();
					if(expiryDate.length()>0){
						System.out.println("inside expiry date if loop");
					tradeObject.set("ExpiryDate", expiryDateNodeList.item(0).getTextContent());
					System.out.println("after expiry date");
					}
				}
				
				NodeList endorsementsNodeList = tradeElement.getElementsByTagName("Endorsements");
				System.out.println("Near Endorsements");
				if(endorsementsNodeList.getLength()>0){
					tradeObject.setString("Endorsements", endorsementsNodeList.item(0).getTextContent());
				}
				else{
					tradeObject.setString("Endorsements", "");
				}
				
				NodeList termsConditionsNodeList = tradeElement.getElementsByTagName("TermsConditions");
				System.out.println("Near TermsConditions");
				if(termsConditionsNodeList.getLength()>0){
					tradeObject.setString("TermsConditions", termsConditionsNodeList.item(0).getTextContent());
				}
				else{
					tradeObject.setString("TermsConditions", "");
				}
				
				NodeList redSealNumberNodeList = tradeElement.getElementsByTagName("RedSealNumber");
				System.out.println("Near RedSealNumber");
				if(redSealNumberNodeList.getLength()>0){
					tradeObject.setString("RedSealNumber", redSealNumberNodeList.item(0).getTextContent());
				}
				else{
					tradeObject.setString("RedSealNumber", "");
				}
				
				NodeList redSealDateNodeList = tradeElement.getElementsByTagName("RedSealDate");
				System.out.println("Near RedSealDate");
				if(redSealDateNodeList.getLength()>0){
					tradeObject.setString("RedSealDate", redSealDateNodeList.item(0).getTextContent());
				}
				else{
					tradeObject.setString("RedSealDate", "");
				}
				
				NodeList MTCUIssueDateNodeList = tradeElement.getElementsByTagName("MTCUIssueDate");
				System.out.println("Near MTCUIssueDate");
				if(MTCUIssueDateNodeList.getLength()>0){
					tradeObject.setString("MTCUIssueDate", MTCUIssueDateNodeList.item(0).getTextContent());
				}
				else{
					tradeObject.setString("MTCUIssueDate", "");
				}
			}
			//String coordinatesValue = XPathAPI.selectSingleNode(rdf, "/Trade/ReferenceFile/text()").getNodeValue();
			//System.out.println(coordinatesValue);
			//tradeObject.setString("ReferenceFile", coordinatesValue.split(",")[1]);
			//coordinates.setString("longitude", coordinatesValue.split(",")[0]);
		}catch(Exception e) {
			System.out.println("inside Catch");
			e.printStackTrace(System.out);
		}

		fieldDataObject = tradeObject;
	}

	public HTTPControl getControlParameters() {
		return httpControl;
	}



	public HTTPHeaders getHeaders() {
		return httpHeaders;
	}



	public boolean isBusinessException() {
		return isBusinessException;
	}



	public void setBusinessException(boolean arg0) {
		isBusinessException = arg0;
		
	}



	public void setControlParameters(HTTPControl arg0) {
		httpControl = arg0;
	}



	public void setHeaders(HTTPHeaders arg0) {
		httpHeaders = arg0;
	}



	public void write(HTTPOutputStream httpoutputstream) throws IOException {
		httpoutputstream.write(nativeData.toByteArray());
		
	}



	public void setDataObject(DataObject arg0) throws DataBindingException {
		fieldDataObject = arg0;
	}



	public DataObject getDataObject() throws DataBindingException {
		return fieldDataObject;
	}

}
