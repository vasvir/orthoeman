/* Author: Konstantinos Zagoris
 Image Processing Tools
 */
"use strict";


Object.getPrototypeOf(document.createElement('canvas').getContext('2d')).zag_LoadImage = function(imgurl, data, callback) {
    var ImageObj = new Image();
    //alert(this.drawImage);
    var dfd = new $.Deferred();
    var context = this;
    ImageObj.onload = function () {
        context.drawImage(this, 0, 0);
        if (callback !== undefined) {
            callback(data);
        }
        dfd.resolve(data);
    };
    ImageObj.src = imgurl;
    return dfd.promise();
}

Object.getPrototypeOf(document.createElement('canvas').getContext('2d')).zag_Invert = function(x,y,width,height) {
    var imageData = (y === null) ? x : this.getImageData(x,y,width,height);
    var data = imageData.data;
    for (var i=0; i< data.length; i += 4) {
        data[i] = 255 - data[i];    // red
        data[i+1] = 255 - data[i+1]; // green
        data[i+2] = 255 -data[i+2]; // blue
    }
    this.putImageData(imageData,0,0);
}

Object.getPrototypeOf(document.createElement('canvas').getContext('2d')).zag_Brightening = function(bvalue,x,y,width,height) {
    var imageData = (y === null) ? x : this.getImageData(x,y,width,height);
    var data = imageData.data;
    //alert(bvalue < 0.0);
    var r, g,b;
    for (var i=0; i< data.length; i += 4) {
        r = data[i]/255;
        g = data[i+1]/255;
        b = data[i+2]/255;
        data[i] = 255*((bvalue< 0.0) ?  r*(1.0 + bvalue) : r + ((1-r)*bvalue));
        data[i+1] = 255*((bvalue< 0.0) ?  g*(1.0 + bvalue) : g + ((1-g)*bvalue));
        data[i+2] = 255*((bvalue< 0.0) ?  b*(1.0 + bvalue) : b + ((1-b)*bvalue));
    }
    this.putImageData(imageData,0,0);
}

Object.getPrototypeOf(document.createElement('canvas').getContext('2d')).zag_BCI = function(bvalue,cvalue,invert,x,y,width,height) {
    var imageData = (y === null) ? x : this.getImageData(x,y,width,height);
    var data = imageData.data;
    //alert(bvalue < 0.0);
    var r, g,b;
    for (var i=0; i< data.length; i += 4) {
        r = data[i]/255;
        g = data[i+1]/255;
        b = data[i+2]/255;
        data[i] = 255*((bvalue< 0.0) ?  r*(1.0 + bvalue) : r + ((1-r)*bvalue));
        data[i+1] = 255*((bvalue< 0.0) ?  g*(1.0 + bvalue) : g + ((1-g)*bvalue));
        data[i+2] = 255*((bvalue< 0.0) ?  b*(1.0 + bvalue) : b + ((1-b)*bvalue));
        data[i] = 255*((data[i]/255 - 0.5)*(Math.tan((cvalue+1)*Math.PI/4)) + 0.5);
        data[i+1] = 255*((data[i+1]/255 - 0.5)*(Math.tan((cvalue+1)*Math.PI/4)) + 0.5);
        data[i+2] = 255*((data[i+2]/255 - 0.5)*(Math.tan((cvalue+1)*Math.PI/4)) + 0.5);
        if (invert) {
            data[i] = 255 - data[i];    // red
            data[i+1] = 255 - data[i+1]; // green
            data[i+2] = 255 -data[i+2]; // blue
        }
    }
    this.putImageData(imageData,0,0);
}

Object.getPrototypeOf(document.createElement('canvas').getContext('2d')).zag_Contrast = function(cvalue,x,y,width,height) {
    var imageData = (y === null) ? x : this.getImageData(x,y,width,height);
    var data = imageData.data;
    for (var i=0; i< data.length; i += 4) {
        data[i] = 255*((data[i]/255 - 0.5)*(Math.tan((cvalue+1)*Math.PI/4)) + 0.5);
        data[i+1] = 255*((data[i+1]/255 - 0.5)*(Math.tan((cvalue+1)*Math.PI/4)) + 0.5);
        data[i+2] = 255*((data[i+2]/255 - 0.5)*(Math.tan((cvalue+1)*Math.PI/4)) + 0.5);
    }
    this.putImageData(imageData,0,0);
}

Object.getPrototypeOf(document.createElement('canvas')).zag_Clone = function ()
{
    //create a new canvas
    var newCanvas = document.createElement('canvas');
    newCanvas.width = this.width;
    newCanvas.height = this.height;
    var context = newCanvas.getContext('2d');

    //apply the old canvas to the new one
    context.drawImage(this, 0, 0);

    //return the new canvas
    return newCanvas;
}

