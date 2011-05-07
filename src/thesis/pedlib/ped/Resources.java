package thesis.pedlib.ped;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import thesis.pedlib.util.MediaType;
import thesis.pedlib.util.MediaTypeService;

/**
 * All the resources that make up the document.
 * XHTML files, images and ped xml documents must be here.
 * 
 *
 */

public class Resources {

	private static final String IMAGE_PREFIX = "image_";
	private static final String ITEM_PREFIX = "item_";
	
	private Map<String, Resource> resources = new HashMap<String, Resource>();
	
	/**
	 * Adds a resource to the resources.
	 * 
	 * Fixes the resources id and href if necessary.
	 * 
	 * @param resource
	 * @return
	 */
	public Resource add(Resource resource) {
		fixResourceHref(resource);
		fixResourceId(resource);
		this.resources.put(resource.getHref(), resource);
		return resource;
	}

	/**
	 * Checks the id of the given resource and changes to a unique identifier if it isn't one already.
	 * 
	 * @param resource
	 */
	public void fixResourceId(Resource resource) {
		String  resourceId = resource.getId();
		
		// first try and create a unique id based on the resource's href
		if (resource.getId() == null || resource.getId() == "" ) {
			String temp = resource.getHref();
			resourceId = temp.substring(0,temp.lastIndexOf("."));
			resourceId = resourceId.substring(0,resourceId.lastIndexOf("/") +1);
		}
		
		resourceId = makeValidId(resourceId, resource);
		
		// check if the id is unique. if not: create one from scratch
		if ((resourceId == "" || resourceId == null) || containsId(resourceId)) {
			resourceId = createUniqueResourceId(resource);
		}
		resource.setId(resourceId);
	}

	/**
	 * Check if the id is a valid identifier. if not: prepend with valid identifier
	 * 
	 * @param resource
	 * @return
	 */
	private String makeValidId(String resourceId, Resource resource) {
		if (! (resourceId == "" || resourceId == null) && ! Character.isJavaIdentifierStart(resourceId.charAt(0))) {
			resourceId = getResourceItemPrefix(resource) + resourceId;
		}
		return resourceId;
	}
	
	private String getResourceItemPrefix(Resource resource) {
		String result;
		if (MediaTypeService.isBitmapImage(resource.getMediaType())) {
			result = IMAGE_PREFIX;
		} else {
			result = ITEM_PREFIX;
		}
		return result;
	}
	
	
	private String createUniqueResourceId(Resource resource) {
		int counter = 1;
		String prefix = getResourceItemPrefix(resource);
		String result = prefix + counter;
		while (containsId(result)) {
			result = prefix + (++ counter);
		}
		return result;
	}

	public boolean containsId(String id) {
		if ((id == "" || id == null)) {
			return false;
		}
		for (Resource resource: resources.values()) {
			if (id.equals(resource.getId())) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Gets the resource with the given id.
	 * 
	 * @param id
	 * @return null if not found
	 */
	public Resource getById(String id) {
		if ((id == "" || id == null)) {
			return null;
		}
		for (Resource resource: resources.values()) {
			if (id.equals(resource.getId())) {
				return resource;
			}
		}
		return null;
	}
	
	/**
	 * Remove the resource with the given href.
	 * 
	 * @param href
	 * @return the removed resource, null if not found
	 */
	public Resource remove(String href) {
		return resources.remove(href);
	}
	
	private void fixResourceHref(Resource resource) {
		if(! (resource.getHref() == "" || resource.getHref() == null)
				&& ! resources.containsKey(resource.getHref())) {
			return;
		}
		if(resource.getHref() == "" || resource.getHref() == null) {
			if(resource.getMediaType() == null) {
				throw new IllegalArgumentException("Resource must have either a MediaType or a href");
			}
			int i = 1;
			String href = createHref(resource.getMediaType(), i);
			while(resources.containsKey(href)) {
				href = createHref(resource.getMediaType(), (++i));
			}
			resource.setHref(href);
		}
	}
	
	private String createHref(MediaType mediaType, int counter) {
		if(MediaTypeService.isBitmapImage(mediaType)) {
			return "image_" + counter + mediaType.getDefaultExtension();
		} else {
			return "item_" + counter + mediaType.getDefaultExtension();
		}
	}
	
	
	public boolean isEmpty() {
		return resources.isEmpty();
	}
	
	/**
	 * The number of resources
	 * @return
	 */
	public int size() {
		return resources.size();
	}
	
	/**
	 * The resources that make up this book.
	 * Resources can be xhtml pages, images, xml documents, etc.
	 * 
	 * @return
	 */
	public Map<String, Resource> getResourceMap() {
		return resources;
	}
	
	public Collection<Resource> getAll() {
		return resources.values();
	}
	
	
	/**
	 * Whether there exists a resource with the given href
	 * @param href
	 * @return
	 */
	public boolean containsByHref(String href) {
		if (href == "" || href == null) {
			return false;
		}
		return resources.containsKey(href);
	}
	
	/**
	 * Sets the collection of Resources to the given collection of resources
	 * 
	 * @param resources
	 */
	public void set(Collection<Resource> resources) {
		this.resources.clear();
		addAll(resources);
	}
	
	/**
	 * Adds all resources from the given Collection of resources to the existing collection.
	 * 
	 * @param resources
	 */
	public void addAll(Collection<Resource> resources) {
		for(Resource resource: resources) {
			fixResourceHref(resource);
			this.resources.put(resource.getHref(), resource);
		}
	}

	/**
	 * Sets the collection of Resources to the given collection of resources
	 * 
	 * @param resources A map with as keys the resources href and as values the Resources
	 */
	public void set(Map<String, Resource> resources) {
		this.resources = new HashMap<String, Resource>(resources);
	}
	
	
	/**
	 * First tries to find a resource with as id the given idOrHref, if that 
	 * fails it tries to find one with the idOrHref as href.
	 * 
	 * @param idOrHref
	 * @return
	 */
	public Resource getByIdOrHref(String idOrHref) {
		Resource resource = getById(idOrHref);
		if (resource == null) {
			resource = getByHref(idOrHref);
		}
		return resource;
	}
	
	
	/**
	 * Gets the resource with the given href.
	 * If the given href contains a fragmentId then that fragment id will be ignored.
	 * 
	 * @param href
	 * @return null if not found.
	 */
	public Resource getByHref(String href) {
		if (href == "" || href == null) {
			return null;
		}
		Resource result = resources.get(href);
		return result;
	}
	
	/**
	 * Gets the first resource (random order) with the give mediatype.
	 * 
	 * Useful for looking up the table of contents as it's supposed to be the only resource with NCX mediatype.
	 * 
	 * @param mediaType
	 * @return
	 */
	public Resource findFirstResourceByMediaType(MediaType mediaType) {
		return findFirstResourceByMediaType(resources.values(), mediaType);
	}
	
	/**
	 * Gets the first resource (random order) with the give mediatype.
	 * 
	 * Useful for looking up the table of contents as it's supposed to be the only resource with NCX mediatype.
	 * 
	 * @param mediaType
	 * @return
	 */
	public static Resource findFirstResourceByMediaType(Collection<Resource> resources, MediaType mediaType) {
		for (Resource resource: resources) {
			if (resource.getMediaType() == mediaType) {
				return resource;
			}
		}
		return null;
	}

	public Collection<String> getAllHrefs() {
		return resources.keySet();
	}
}
