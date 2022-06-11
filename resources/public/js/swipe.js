(function() {

    const swipe = {
        init
    };

    window.swipe = swipe;


    let touchstartX = 0;
    let touchstartY = 0;
    let touchendX = 0;
    let touchendY = 0;


    function handleGesture(touchstartX, touchstartY, touchendX, touchendY, minPixels) {
        const delx = touchendX - touchstartX;
        const dely = touchendY - touchstartY;

        if(Math.abs(delx) > Math.abs(dely)){
            const longEnough = minPixels ? Math.abs(delx) > minPixels : true;

            if(delx > 0 && longEnough) {
                console.log('delx', delx);
                return "right";
            }
            else if (longEnough) {
                return "left";
            } else { return "tap"; }
        }
        else if(Math.abs(delx) < Math.abs(dely)){
            const longEnough = minPixels ? Math.abs(dely) > minPixels : true;

            if(dely > 0 && longEnough) { return "down"; }
            else if (longEnough) { return "up"; }
            else { return "tap"; }
        }
        else { return "tap"; }
    }


    function init (elementId, minPixels) {
        const gestureZone = document.getElementById(elementId);

        gestureZone.addEventListener('touchstart', function(event) {
            touchstartX = event.changedTouches[0].screenX;
            touchstartY = event.changedTouches[0].screenY;
        }, false);

        gestureZone.addEventListener('touchend', function(event) {
            touchendX = event.changedTouches[0].screenX;
            touchendY = event.changedTouches[0].screenY;
            const direction = handleGesture(touchstartX, touchstartY, touchendX, touchendY, minPixels);

            if (direction !== 'tap') {
                htmx.trigger(event.target, "swipe", { [direction]: true, direction });
            }

        }, false);
    }
})();
