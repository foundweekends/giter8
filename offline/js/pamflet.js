var Pamflet = Pamflet || { page: {} };
$(function() {
    var load = function() {
        window.location = this.href;
    };
    var prev = function() {
        $("a.page.prev").first().each(load);
    };
    var next = function() {
        $("a.page.next").first().each(load);
    }
    $(document).keyup(function (event) {
        if (event.altKey || event.ctrlKey || event.shiftKey || event.metaKey)
            return;
        if (event.keyCode == 37) {
            prev();
        } else if (event.keyCode == 39) {
            next();
        }
    });
    var getSelected = function() {
        if (window.getSelection) {
            var retval = window.getSelection()
            if (retval.anchorNode && retval.focusNode && retval.toString
                && (retval.rangeCount > 0) && !retval.isCollapsed) return retval
            else return false;
        }
        return false;
    }
    $(document).mouseup(function (event) {
        if ($("div.highlight-outer").css("display") !== "block") {
            return;
        }
        var selected = getSelected();
        if (selected) {
            var r = selected.getRangeAt(0);
            if (r.getBoundingClientRect) {
                $("div.highlight-menu").show();
                var rect = r.getBoundingClientRect();
                var left = Math.round(rect.left + window.scrollX);
                var top = Math.round(rect.top + window.scrollY - 1.25 * $("div.highlight-menu").css("height").replace(/[^\d\.]/g,''))
                $("div.highlight-outer").css("left", left).css("top", top);
            } else {
                $("div.highlight-menu").hide();
            }
        } else {
            $("div.highlight-menu").hide();
        }
    });
    $("button#highlight-button-twitter").click(function (event) {
        var selected = getSelected();
        if (selected) {
            var byline = "";
            if (Pamflet.twitter && ("show" !== Pamflet.twitter)) {
                byline = Pamflet.twitter;
            }
            byline = " —" + byline + " ";
            var txt = selected.toString();
            if (txt.length + byline.length + 24 > 140) {
                txt = txt.substring(0, 140 - 24 - 1 - byline.length) + "…";
            }
            var tweet = "“" + txt + "”" + byline + window.location.href;
            var lang = Pamflet.page.language;
            window.open("https://twitter.com/intent/tweet?text=" + encodeURIComponent(tweet) + "&lang=" + lang,
                "_blank", "width=550,height=450,directories=0,menubar=0,status=0,toolbar=0");
        }
    })
    var show_message = "show table of contents";
    var hide_message = "hide table of contents";
    $(".collap").collapse({
        "head": "h4",
        show: function () {
            this.animate({ 
                height: "toggle"
            }, 300);
            this.prev(".toctitle").children("a").text(hide_message);
        },
        hide: function () {
            this.animate({
                height: "toggle"
            }, 300);
            this.prev(".toctitle").children("a").text(show_message); 
        }
    });
    $(".collap a.tochead").show();
    $(".collap a.tochead").click(function(event){
        $(".toctitle").children("a").click();
    });
    $(".collap .toctitle a").text(show_message);

    var getParameterByName = function(name) {
        name = name.replace(/[\[]/, "\\\[").replace(/[\]]/, "\\\]");
        var regex = new RegExp("[\\?&]" + name + "=([^&#]*)"),
            results = regex.exec(location.search);
        return results == null ? "" : decodeURIComponent(results[1].replace(/\+/g, " "));
    }
    var color_scheme = getParameterByName("color_scheme");
    if (color_scheme) {
        $("body").attr("class", "color_scheme-" + color_scheme);
        if (window.localStorage) {
            window.localStorage.color_scheme = color_scheme;
        }
    } else {
        if (window.localStorage) {
            if (window.localStorage.color_scheme) {
                $("body").attr("class", "color_scheme-" + window.localStorage.color_scheme);
            }
        }
    }
});
