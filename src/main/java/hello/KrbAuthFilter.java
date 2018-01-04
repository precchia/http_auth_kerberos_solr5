package hello;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrException.ErrorCode;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.NodeConfig;
import org.apache.solr.core.NodeConfig.NodeConfigBuilder;
import org.apache.solr.core.SolrResourceLoader;
import org.apache.solr.security.AuthenticationPlugin;
import org.apache.solr.security.KerberosPlugin;
import org.apache.solr.security.PKIAuthenticationPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;
/*
 * https://github.com/apache/lucene-solr/blob/releases/lucene-solr/5.5.2/solr/core/src/java/org/apache/solr/core/CoreContainer.java
 * https://github.com/apache/lucene-solr/blob/releases/lucene-solr/5.5.2/solr/core/src/java/org/apache/solr/core/CoreContainer.java#L416
 * https://github.com/apache/lucene-solr/blob/releases/lucene-solr/5.5.2/solr/core/src/java/org/apache/solr/core/NodeConfig.java
 * https://github.com/apache/lucene-solr/blob/releases/lucene-solr/5.5.2/solr/core/src/java/org/apache/solr/core/SolrResourceLoader.java
 * https://github.com/apache/lucene-solr/blob/releases/lucene-solr/5.5.2/solr/core/src/java/org/apache/solr/servlet/HttpSolrCall.java
 * https://github.com/apache/lucene-solr/blob/releases/lucene-solr/5.5.2/solr/core/src/java/org/apache/solr/security/KerberosPlugin.java
 * https://github.com/apache/lucene-solr/blob/releases/lucene-solr/5.5.2/solr/core/src/java/org/apache/solr/security/AuthenticationPlugin.java
 * https://github.com/apache/lucene-solr/blob/releases/lucene-solr/5.5.2/solr/core/src/java/org/apache/solr/core/SolrCore.java
 * https://github.com/apache/lucene-solr/blob/releases/lucene-solr/5.5.2/solr/core/src/java/org/apache/solr/servlet/HttpSolrCall.java
 * https://github.com/apache/lucene-solr/blob/releases/lucene-solr/5.5.2/solr/core/src/java/org/apache/solr/servlet/SolrDispatchFilter.java
 * Client communication:
 * https://github.com/apache/lucene-solr/blob/releases/lucene-solr/5.5.2/solr/core/src/java/org/apache/solr/servlet/HttpSolrCall.java
 * https://github.com/apache/lucene-solr/blob/releases/lucene-solr/5.5.2/solr/core/src/java/org/apache/solr/servlet/HttpSolrCall.java#L561
 * https://github.com/apache/lucene-solr/blob/releases/lucene-solr/5.5.2/solr/core/src/java/org/apache/solr/servlet/SolrDispatchFilter.java#L136
 * https://github.com/apache/lucene-solr/blob/releases/lucene-solr/5.5.2/solr/core/src/java/org/apache/solr/handler/component/ShardHandlerFactory.java
 * https://github.com/apache/lucene-solr/blob/releases/lucene-solr/5.5.2/solr/core/src/java/org/apache/solr/handler/component/HttpShardHandler.java#L159
 * Selects managed by HttpShardHandler.submit
 * https://github.com/apache/lucene-solr/blob/releases/lucene-solr/5.5.2/solr/core/src/java/org/apache/solr/handler/component/HttpShardHandlerFactory.java
 * 
 * SearchComponent:
 * https://github.com/apache/lucene-solr/blob/releases/lucene-solr/5.5.2/solr/core/src/java/org/apache/solr/handler/component/SearchComponent.java
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class KrbAuthFilter implements Filter,ApplicationContextAware {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private ApplicationContext ctx;
	@Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.ctx = applicationContext;
    }
	
	AuthenticationPlugin authenticationPlugin;
	private CoreContainer cores;
	
	/*
	void initializeAuthenticationPlugin() {
		authenticationPlugin = new KerberosPlugin();
	}
	 */
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		// Using standard CoreContainer: we will search for a file called security.json within root_dir/hello (no resources)
		/* 
		 * authenticationPlugin taken from system properties (-PauthenticationPlugin=org.apache.solr.security.KerberosPlugin)
		 * rather than from escurity.json
		 * See: 
		 * https://github.com/apache/lucene-solr/blob/8e5d40b22a3968df065dfc078ef81cbb031f0e4a/solr/core/src/java/org/apache/solr/core/CoreContainer.java#L416
		 */
		cores = Application.getInstance().getCoreContainer();
		
		log.info("security.json:" + Paths.get(cores.getSolrHome()).resolve("security.json"));
		log.info ("Authentication plugin is:" + cores.getAuthenticationPlugin());
		log.info ("updateShardHalder:" + cores.getUpdateShardHandler());
		log.info ("updateShardHalder.HttpClient:" + cores.getUpdateShardHandler().getHttpClient());
		log.info("ShardHandlerFactory: " + cores.getShardHandlerFactory());
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		// Highly stripped down version of solr doFilter
		AtomicReference<ServletRequest> wrappedRequest = new AtomicReference<>();
		log.info("Authenticating");
	    if (!authenticateRequest(request, response, wrappedRequest)) { // the response and status code have already been sent
	    	return;
	    }
    	// Mocking everything after the authenticateRequest
		log.info("Authenticated");
	    chain.doFilter(request, response);
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		
	}
	
	private boolean authenticateRequest(ServletRequest request, ServletResponse response, final AtomicReference<ServletRequest> wrappedRequest) throws IOException {
	    final AtomicBoolean isAuthenticated = new AtomicBoolean(false);
	    AuthenticationPlugin authenticationPlugin = cores.getAuthenticationPlugin();
	    if (authenticationPlugin == null) {
	      return true;
	    } else {
	      //special case when solr is securing inter-node requests
	      String header = ((HttpServletRequest) request).getHeader(PKIAuthenticationPlugin.HEADER);
	      if (header != null && cores.getPkiAuthenticationPlugin() != null)
	        authenticationPlugin = cores.getPkiAuthenticationPlugin();
	      try {
	        log.debug("Request to authenticate: {}, domain: {}, port: {}", request, request.getLocalName(), request.getLocalPort());
	        // upon successful authentication, this should call the chain's next filter.
	        authenticationPlugin.doAuthenticate(request, response, new FilterChain() {
	          public void doFilter(ServletRequest req, ServletResponse rsp) throws IOException, ServletException {
	            isAuthenticated.set(true);
	            wrappedRequest.set(req);
	          }
	        });
	      } catch (Exception e) {
	        e.printStackTrace();
	        throw new SolrException(ErrorCode.SERVER_ERROR, "Error during request authentication, ", e);
	      }
	    }
	    // failed authentication?
	    if (!isAuthenticated.get()) {
	      response.flushBuffer();
	      return false;
	    }
	    return true;
	}
}
