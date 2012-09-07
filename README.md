h1. Wiki

h1. Web Services

h2. Cached Location Search

This translates the received search request into different Social Network API queries based on location, gathers all the responses (in JSON format), extracts relevant data and organizes each result in XML according to W3C POI WG.

To improve performance it features caching. It has one cache for each source, cache elements are Priority Queues of POIs sorted by timestamp, cache keys are concatenation of query and location parameters (coordinate is truncated in order to abstract small variations in latitude and longitude).

Each source has a _refresh-interval_ which indicate the minimum interval between two requests with same cache queue in order to avoid flooding.

Sending requests to a source API is synchronized by cache key, in order to avoid sending two or more similar requests in a short period of time, before any of them receives a response. In such case, only first request is sent to source and the others are blocked until response is received and cache updated in case of successful response.

When rate limited by any source API, error message is identified and Webservice suspends requests to the correspondent source. During suspension time the Webservice uses cached data if available.

** *Address*: 
*** _/socialmediaservice/cachedsearch_

** *Parameters*
*** +query+: The query string
- Mandatory parameter
*** +location+: Comma separated coordinate in the form _latitude,longitude_
- Mandatory parameter
- _Latitude_ should be contained in the interval [-90,90] and _longitude_ in [-180,180]
*** +radius+: Defines the search area from as a circle centered in the location given (in Kilometers)
- Optional parameter
- If absent or empty value set by _dft-radius_ in _search.properties_ is used as default
- Float greater than zero and smaller than value set by _dft-max-results_ in _search.properties_
- For Flickr's maximum value is 20km
*** +max-results+: Maximum number of result entries per source
- Optional Parameter
- Integer between 1 and 50
- If absent or empty value set by _dft-max-results_ in _search.properties_ is used as default
*** +sources+: Specifies which social networks should be queried
- Optional parameter
- Available sources: _youtube_, _twitter_, _facebook_, _flickr_
- If absent or empty uses all sources
- Subsets specified as comma separated values (invalid values will be ignored)
*** +lang+: Language of the search results in the format _gg_CC_ (_gg_ = ISO 639 Language code, _CC_ = ISO 3166-1 alpha-2 Country Code)
- Optional parameter
- Doesn’t apply for Flickr
- If absent or empty, allow any language
- Set of allowed languages is in _lang.csv_ (https://redmine.viscenter.de/projects/socialmediaservice/repository/revisions/master/entry/socialmediaservice/src/main/resources/lang.csv) which is based on Facebook Locales
- Twitter and Youtube only uses the language code, thus for them country code is ignored
*** *_Examples_*:
***# /socialmediaservice/cachedsearch?q=Crested+Butte+Colorado&location=38.869,-106.987&radius=70km&max-results=20&sources=flickr
***# /socialmediaservice/cachedsearch?q=Holton+Indiana&location=39.076,-85.385&max-results=15&sources=facebook,twitter
***# /socialmediaservice/cachedsearch?q=Panthessaliko+Stadium&location=39.387,22.931&radius=65km&max-results=15&lang=en_US&sources=youtube

** *Configuration Files*
*** _search.properties_ (https://redmine.viscenter.de/projects/socialmediaservice/repository/revisions/master/entry/socialmediaservice/src/main/resources/search.properties)
**** +${source}-url+: the source base URL
**** +${source}-const-params+: the constant parameters required by each source
**** +${source}-refresh-interval+: the minimum time between two similar requests (same _query_ and _location_ parameters), used to avoid flooding
**** +flickr-extras+: extra fields in the response, which must be explicitely requested (_description, date_upload, owner_name, original_format, geo, tags, media, path_alias, url_sq, url_o, url_t, url_z, icon_server_)
**** +flickr-api-key+: the API Key, which is required by Flickr for using the API
**** +dft-max-results+: the default value assigned to _max-result_ parameter when absent or empty in the request
**** +dft-radius+: the default value assigned to _radius_ parameter when absent or empty in the request
**** +dft-max-radius+: the maximum value of _radius_ parameter
*** _cache.ccf_ (https://redmine.viscenter.de/projects/socialmediaservice/repository/revisions/master/entry/socialmediaservice/src/main/resources/cache.ccf)
**** Information about JCS configuration: http://commons.apache.org/jcs/BasicJCSConfiguration.html

** *Error Messages*
**** 400: Bad Request
***** Query parameter missing, it's a mandatory non-empty parameter	
***** Location parameter missing, it's a mandatory non-empty parameter	
***** Invalid parameter: location = _${location}_, it should be latitude[-180,180],longitude[-90,90]. Example: location=37.42307,-122.08427 
***** Invalid parameter: radius = _${radius}_, it should contain a float distance in kilometers with optional unit. Example: radius=23.5km
***** Invalid parameter: max-results = _${maxResults}_, it should be a valid positive not greater than 50
***** Invalid parameter: language = _${language}_, it should be in the format 'gg_CC' where gg = ISO 639 Language code, CC = ISO 3166-1 alpha-2 Country Code
***** Invalid parameter sources = _${sources}_, it should be a CSV subset of {twitter,youtube,facebook,flickr}. Example: sources=twitter

** *Libraries Used*
*** *Apache JCS:* http://commons.apache.org/jcs/
*** *XStream*: http://xstream.codehaus.org/index.html
*** *JSON*: http://www.json.org/java/
*** *Apache HttpComponents*: http://hc.apache.org/index.html 

** *Important Design Decisions*

** *Observed Abnormal Behaviours*
*** *Facebook*: 
- Its API is extremely slow, with response time frequently exceeding 4 seconds
- Many of the returned posts are shares of the same original post. The Websevice ignores the share posts and adds only original shared post
- When rate limit, sends proper error message but no _retry-after_ to indicate suspension time (only Twitter informs it)
*** *Flickr*
- Its API is not very stable, it's expected to present very high average reponse time and variance. 
- It doesns't have a specific error response for Rate Limiting
*** *Twitter*: 
- Sometimes response body is corrupted (the Web Service tries to recover the response to a valid JSON format with the uncorrupted result entries)
*** *Youtube*
- Many of the returned results doesn't have a location Coordinate
- Like in Facebook, when rate limited, sends proper error message but no _retry-after_ to indicate suspension time

h2. Twitter Streaming

This service streams POIs obtained from Twitter's firehose that belong to the tiles requested by the user.

** *Address*: 
*** _/socialmediaservice/stream_

** *Parameters*
*** +tiles+: A set of maptiles separated by comma. Each map tile is composed by 3 integers also separated by comma.
- Mandatory parameter
*** *_Examples_*:
***# /socialmediaservice/stream?tiles=511,340,10
***# /socialmediaservice/stream?tiles=526,336,10,162,399,10,301,387,10,527,359,10,815,529,10,511,340,10


** *Configuration Files*
*** _src/main/resources/twitter.properties_
**** _resource_url_: Twitter stream base url (https://stream.twitter.com/1/statuses/firehose.json)
**** _username_: Username of the Twitter account accessing the stream. (Twitter gives access to authenticated users even though there's only public data in the stream)
**** _password_: Corresponding password
**** _buffer_size_: Size of the circular buffer used for communication between producer and consumer threads


Output format should be according to W3C POI WG:
* http://www.w3.org/2010/POI/
* http://www.w3.org/2010/POI/wiki/Main_Page
