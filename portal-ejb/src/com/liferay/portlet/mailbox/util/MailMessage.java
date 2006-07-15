/**
 * Copyright (c) 2000-2006 Liferay, LLC. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.liferay.portlet.mailbox.util;

import java.util.ArrayList;
import java.util.List;

import javax.mail.Address;

import com.liferay.util.GetterUtil;
import com.liferay.util.Html;
import com.liferay.util.StringPool;
import com.liferay.util.Validator;

/**
 * <a href="MailMessage.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Alexander Chow
 *
 */
public class MailMessage {
	
	public Address getFrom() {
		return _from;
	}
	
	public void setFrom(Address from) {
		_from = from;
	}
	
	public Address [] getTo() {
		return _to;
	}
	
	public void setTo(Address [] to) {
		_to = to;
	}
	
	public Address [] getCc() {
		return _cc;
	}
	
	public void setCc(Address [] cc) {
		_cc = cc;
	}

	public Address [] getBcc() {
		return _bcc;
	}
	
	public void setBcc(Address [] bcc) {
		_bcc = bcc;
	}

    public String getSubject() {
    	return _subject;
    }
    
    public void setSubject(String subject) {
    	_subject = subject;
    }

    public String getPlainBody() {
    	if (Validator.isNotNull(_plainBody)) {
        	return _plainBody;
    	}
    	else {
    		return Html.stripHtml(_htmlBody);
    	}
    }

    public void appendPlainBody(String plainBody) {
    	if (Validator.isNull(_plainBody)) {
    		_plainBody = plainBody;
    	}
    	else {
    		_plainBody += StringPool.NEW_LINE + StringPool.NEW_LINE + plainBody;
    	}
    }
    
    public void setPlainBody(String plainBody) {
    	_plainBody = plainBody;
    }

    public String getHtmlBody() { 
    	if (Validator.isNotNull(_htmlBody)) {
        	return _htmlBody;
    	}
    	else {
    		return "<PRE>" + GetterUtil.getString(_plainBody) + "</PRE>";
    	}
    }

    public void appendHtmlBody(String htmlBody) {
    	if (Validator.isNull(_htmlBody)) {
    		_htmlBody = htmlBody;
    	}
    	else {
    		_htmlBody += "<HR/>" + htmlBody;
    	}
    }

    public void setHtmlBody(String htmlBody) {
    	_htmlBody = htmlBody;
    }

	public List getAttachments() {
		return _attachments;
	}

	public void appendAttachment(MailAttachment ma) {
		_attachments.add(ma);
	}
	
	public boolean isSimple() {
		if (Validator.isNotNull(_htmlBody) || !_attachments.isEmpty()) {
			return false;
		}
		return true;
	}
	
	private Address _from;

	private Address [] _to;
	
	private Address [] _cc;
	
	private Address [] _bcc;
	
	private String _subject;
	
	private String _plainBody;
	
	private String _htmlBody;
	
	private List _attachments = new ArrayList();

}