package org.orienteer.oposter.facebook;

import java.util.Date;
import java.util.List;

import org.orienteer.core.OrienteerWebApplication;
import org.orienteer.core.component.visualizer.UIVisualizersRegistry;
import org.orienteer.core.dao.DAO;
import org.orienteer.core.dao.DAOField;
import org.orienteer.core.dao.DAOOClass;
import org.orienteer.core.dao.ODocumentWrapperProvider;
import org.orienteer.oposter.model.IChannel;
import org.orienteer.oposter.model.IContent;
import org.orienteer.oposter.model.IImageAttachment;
import org.orienteer.oposter.model.IPlatformApp;

import com.google.inject.ProvidedBy;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.restfb.BinaryAttachment;
import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.FacebookClient.AccessToken;
import com.restfb.Parameter;
import com.restfb.types.GraphResponse;
import com.restfb.Version;

@ProvidedBy(ODocumentWrapperProvider.class)
@DAOOClass(value = IFacebookApp.CLASS_NAME, orderOffset = 100)
public interface IFacebookApp extends IPlatformApp{
	public static final String CLASS_NAME = "OPFacebookApp";
	
	@DAOField(notNull = true)
	public String getAppId();
	public void setAppId(String value);
	
	@DAOField(notNull = true, visualization = UIVisualizersRegistry.VISUALIZER_PASSWORD)
	public String getAppSecret();
	public void setAppSecret(String value);
	
	@DAOField(uiReadOnly = true)
	public String getAppAccessToken();
	public void setAppAccessToken(String value);
	
	@DAOField(type = OType.DATETIME, uiReadOnly = true)
	public Date getAppAccessTokenExpires();
	public void setAppAccessTokenExpires(Date value);
	
	
	@Override
	public default boolean send (IChannel channel, IContent content) {
		if(channel instanceof IFacebookPage) {
			IFacebookPage fp = (IFacebookPage) channel;
			FacebookClient facebookClient = getFacebookClient().createClientWithAccessToken(fp.getPageAccessToken());
			List<IImageAttachment> images = content.getImages();
			if(images==null || images.isEmpty()) {
				facebookClient.publish(fp.getPageId()+"/feed", GraphResponse.class, Parameter.with("message", content.getContent()));
			} else {
				IImageAttachment image = images.get(0);
				facebookClient.publish(fp.getPageId()+"/photos", 
									   GraphResponse.class,
									   BinaryAttachment.with(image.getName(), image.getData(), image.detectContentType()),
									   Parameter.with("message", content.getContent()));
			}
		}
		return false;
	}
	
	
	public default FacebookClient getFacebookClient() {
		String key = IFacebookApp.class.getSimpleName()+DAO.asDocument(this).getIdentity();
		FacebookClient ret = OrienteerWebApplication.lookupApplication().getMetaData(key);
		if(ret==null) {
			String appAccessToken = getAppAccessToken();
			Date expires = getAppAccessTokenExpires();
			if(appAccessToken!=null && expires!=null && expires.compareTo(new Date())>0) {
				ret = new DefaultFacebookClient(appAccessToken, Version.LATEST);
			} else {
				ret = new DefaultFacebookClient(Version.LATEST);
				AccessToken token = ret.obtainAppAccessToken(getAppId(), getAppSecret());
				setAppAccessToken(token.getAccessToken());
				setAppAccessTokenExpires(token.getExpires());
				DAO.save(this);
				ret = token.getClient();
			}
			OrienteerWebApplication.lookupApplication().setMetaData(key, ret);
		}
		return ret;
	}
}