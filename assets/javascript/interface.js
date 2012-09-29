var reader;
var curPage = 1;
var curPercentage = 0.0;
var index;

/**
 * Initialize the reader element.
 */
Monocle.Events.listen(window, 'load', function() {
	reader = Monocle.Reader('reader', null, {
	  stylesheet: 'body.day, body.day * {color:#000 !important;} body.night, body.night * {color:#DDD !important;}',//for night/day mode
		flipper : Monocle.Flippers.Instant
	}, function (){
	
	  
	  /* PAGE NUMBER RUNNING HEAD */
	var pageNumber = {
		runners : [],
		createControlElements : function(page) {
			var cntr = document.createElement('div');
			cntr.className = "pageNumber";
			var runner = document.createElement('div');
			runner.className = "runner";
			cntr.appendChild(runner);
			this.runners.push(runner);
			this.update(page);
			return cntr;
		},
		update : function(page) {
			var place = reader.getPlace(page);
			if(place){
			  curPage = place.pageNumber();
			   curPercentage = place.percentageThrough();
			   window.android.setTotalPageNum(place.pagesInComponent());
			   window.android.setCurPageLocation(curPage, curPercentage);
			}
	}
	}
	
	reader.addControl(pageNumber, 'page');
	reader.listen('monocle:pagechange', function(evt) {
	    pageNumber.update(evt.m.page);
	});
	 
	Monocle.Events.listen(window, 'resize', function(){
	  window.reader.resized();
	});
	
	window.reader.listen('monocle:boundarystart', function(evt) {
		window.android.navigate(-1);
	});
	window.reader.listen('monocle:boundaryend', function(evt) {
		window.android.navigate(1);
	});
	
	
	window.reader.listen('monocle:loaded', function(evt) {
		window.android.updateViewValues();
	});
	
	
	});

	
});


/**
 * update monocle settings (font, page etc)
 * 
 * @param percentage
 * @param fontScale
 */
function updateMonocle(percentage, fontScale){
  var place = reader.getPlace();
			if(place){
			   var activePercentage = place.percentageThrough();
			   if(activePercentage != percentage)
			      openPageByPercentage(percentage);
			}
			
			var scale = reader.formatting.getFontScale();
			if((scale == null) || (scale != fontScale))
			    setFontScale(fontScale);
			
}

/**
 * set day or night mode of text
 * 
 * @param isNightMode
 */
function toggleDayNight(isNightMode){
  var frame = reader.dom.find('component');
  if(isNightMode){
    document.getElementById("reader").className = "night";
    frame.contentDocument.getElementsByTagName('body')[0].className = "night";
  }else{
    document.getElementById("reader").className = "day";
    frame.contentDocument.getElementsByTagName('body')[0].className = "day";
  }
}


/**
 * set the font scale used by monocle
 * 
 * @param fontScale
 */
function setFontScale(scale){
  reader.formatting.setFontScale(scale, true);
}


/**
 * Opens by page number
 * 
 * @param pageNum
 */
function openPageByNum(pageNum) {
	reader.moveTo( {
		page : pageNum
	});
}


/**
 * Opens by page percentage
 * 
 * @param percentage
 */
function openPageByPercentage(percentage) {
	reader.moveTo( {
		percent : percentage
	});
}