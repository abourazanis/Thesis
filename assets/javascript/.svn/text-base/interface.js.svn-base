var reader;
var curPage = 1;
var curPercentage = 0.0;

/**
 * Initialize the reader element.
 */
Monocle.addListener(window, 'load', function() {
	reader = Monocle.Reader('reader', null, {
		flipper : Monocle.Flippers.Instant
	});

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
			curPage = place.getLocus().page;
			// curPage = place.pageNumber();
		curPercentage = place.percentageThrough();
	}
	}
	reader.addControl(pageNumber, 'page');
	reader.addListener('monocle:pagechange', function(evt) {
		pageNumber.update(evt.monocleData.page);
	});
});

/**
 * Returns total page number of chapter
 */
function getTotalPageNum() {
	reader.moveTo( {
		percent : 1.0
	});
	window.android.setTotalPageNum(curPage);

	reader.moveTo( {
		page : 1
	});
}

/**
 * Returns total page number of chapter (paging)
 */
function getTotalPageNumPaging(chapter) {
	reader.moveTo( {
		percent : 1.0
	});
	window.android.setTotalPageNumPaging(chapter, curPage);

	reader.moveTo( {
		page : 1
	});
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
	window.android.setCurPageLocation(curPage, curPercentage);
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
	window.android.setCurPageLocation(curPage, curPercentage);
}

/**
 * Opens previous page
 */
function prevPage() {
	reader.moveTo( {
		direction : -1
	});
	window.android.setCurPageLocation(curPage, curPercentage);
}

/**
 * Opens next page
 */
function nextPage() {
	reader.moveTo( {
		direction : 1
	});
	window.android.setCurPageLocation(curPage, curPercentage);
}
