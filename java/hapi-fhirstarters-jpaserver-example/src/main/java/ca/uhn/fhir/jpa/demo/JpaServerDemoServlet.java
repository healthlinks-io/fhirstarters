package ca.uhn.fhir.jpa.demo;

import java.util.Collection;
import java.util.List;

import javax.servlet.ServletException;

import ca.uhn.fhir.rest.api.EncodingEnum;
import org.hl7.fhir.dstu3.model.Meta;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.dao.DaoConfig;
import ca.uhn.fhir.jpa.dao.IFhirSystemDao;
import ca.uhn.fhir.jpa.provider.dstu3.JpaConformanceProviderDstu3;
import ca.uhn.fhir.jpa.provider.dstu3.JpaSystemProviderDstu3;
import ca.uhn.fhir.jpa.search.DatabaseBackedPagingProvider;
import ca.uhn.fhir.narrative.DefaultThymeleafNarrativeGenerator;
import ca.uhn.fhir.rest.server.ETagSupportEnum;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.interceptor.IServerInterceptor;

public class JpaServerDemoServlet extends RestfulServer {

	private static final long serialVersionUID = 1L;

	private WebApplicationContext myAppCtx;

	@SuppressWarnings("unchecked")
	@Override
	protected void initialize() throws ServletException {
		super.initialize();

		/* 
		 * We want to support FHIR DSTU3 format. This means that the server
		 * will use the DSTU3 bundle format and other DSTU3 encoding changes.
		 */
		setFhirContext(FhirContext.forDstu3());

		// Get the spring context from the web container (it's declared in web.xml)
		myAppCtx = ContextLoaderListener.getCurrentWebApplicationContext();

		/* 
		 * The BaseJavaConfigDstu3.java class is a spring configuration
		 * file which is automatically generated as a part of hapi-fhir-jpaserver-base and
		 * contains bean definitions for a resource provider for each resource type
		 */
		String resourceProviderBeanName = "myResourceProvidersDstu3";
		List<IResourceProvider> beans = myAppCtx.getBean(resourceProviderBeanName, List.class);
		setResourceProviders(beans);
		
		/* 
		 * The system provider implements non-resource-type methods, such as
		 * transaction, and global history.
		 */
		Object systemProvider = myAppCtx.getBean("mySystemProviderDstu3", JpaSystemProviderDstu3.class);
		setPlainProviders(systemProvider);

		/*
		 * The conformance provider exports the supported resources, search parameters, etc for
		 * this server. The JPA version adds resource counts to the exported statement, so it
		 * is a nice addition.
		 */
      IFhirSystemDao<org.hl7.fhir.dstu3.model.Bundle, Meta> systemDao = myAppCtx.getBean("mySystemDaoDstu3", IFhirSystemDao.class);
      JpaConformanceProviderDstu3 confProvider = new JpaConformanceProviderDstu3(this, systemDao,
      myAppCtx.getBean(DaoConfig.class));
      confProvider.setImplementationDescription("Example Server");
      setServerConformanceProvider(confProvider);

		/*
		 * Enable ETag Support (this is already the default)
		 */
		setETagSupport(ETagSupportEnum.ENABLED);

		/*
		 * This server tries to dynamically generate narratives
		 */
		FhirContext ctx = getFhirContext();
		ctx.setNarrativeGenerator(new DefaultThymeleafNarrativeGenerator());

		/*
		 * Default to JSON and pretty printing
		 */
		setDefaultPrettyPrint(true);
		setDefaultResponseEncoding(EncodingEnum.JSON);

		/*
		 * This configures the server to page search results to and from
		 * the database, instead of only paging them to memory. This may mean
		 * a performance hit when performing searches that return lots of results,
		 * but makes the server much more scalable.
		 */
		setPagingProvider(myAppCtx.getBean(DatabaseBackedPagingProvider.class));

		/*
		 * Load interceptors for the server from Spring (these are defined in FhirServerConfig.java)
		 */
		Collection<IServerInterceptor> interceptorBeans = myAppCtx.getBeansOfType(IServerInterceptor.class).values();
		for (IServerInterceptor interceptor : interceptorBeans) {
			this.registerInterceptor(interceptor);
		}

		/*
		 * If you are hosting this server at a specific DNS name, the server will try to 
		 * figure out the FHIR base URL based on what the web container tells it, but
		 * this doesn't always work. If you are setting links in your search bundles that
		 * just refer to "localhost", you might want to use a server address strategy:
		 */
		//setServerAddressStrategy(new HardcodedServerAddressStrategy("http://mydomain.com/fhir/baseDstu2"));
		
	}

}
