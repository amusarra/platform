//>>built
define("dojox/dgauges/ScaleIndicatorBase",["dojo/_base/lang","dojo/_base/declare","dojo/on","dojo/_base/connect","dojo/_base/fx","dojox/gfx","dojox/widget/_Invalidating","./IndicatorBase"],function(_1,_2,on,_3,fx,_4,_5,_6){
return _2("dojox.dgauges.ScaleIndicatorBase",_6,{scale:null,value:0,interactionArea:"gauge",interactionMode:"mouse",animationDuration:0,animationEaser:null,_indicatorShapeFuncFlag:true,_interactionAreaFlag:true,_downListeners:null,_cursorListeners:null,_moveAndUpListeners:null,_transitionValue:NaN,_preventAnimation:false,_animation:null,constructor:function(){
this.watch("value",_1.hitch(this,function(){
this.valueChanged(this);
}));
this.watch("value",_1.hitch(this,this._startAnimation));
this.watch("interactionArea",_1.hitch(this,function(){
this._interactionAreaFlag=true;
}));
this.watch("interactionMode",_1.hitch(this,function(){
this._interactionAreaFlag=true;
}));
this.watch("indicatorShapeFunc",_1.hitch(this,function(){
this._indicatorShapeFuncFlag=true;
}));
this.addInvalidatingProperties(["scale","value","indicatorShapeFunc","interactionArea","interactionMode"]);
this._downListeners=[];
this._moveAndUpListeners=[];
this._cursorListeners=[];
},_startAnimation:function(_7,_8,_9){
if(this.animationDuration==0){
return;
}
if(this._animation&&(this._preventAnimation||_8==_9)){
this._animation.stop();
return;
}
this._animation=new fx.Animation({curve:[_8,_9],duration:this.animationDuration,easing:this.animationEaser?this.animationEaser:fx._defaultEasing,onAnimate:_1.hitch(this,this._updateAnimation),onEnd:_1.hitch(this,this._endAnimation),onStop:_1.hitch(this,this._endAnimation)});
this._animation.play();
},_updateAnimation:function(v){
this._transitionValue=v;
this.invalidateRendering();
},_endAnimation:function(){
this._transitionValue=NaN;
this.invalidateRendering();
},refreshRendering:function(){
if(this._gfxGroup==null||this.scale==null){
return;
}else{
if(this._indicatorShapeFuncFlag&&_1.isFunction(this.indicatorShapeFunc)){
this._gfxGroup.clear();
this.indicatorShapeFunc(this._gfxGroup,this);
this._indicatorShapeFuncFlag=false;
}
if(this._interactionAreaFlag){
this._interactionAreaFlag=this._connectDownListeners();
}
}
},valueChanged:function(_a){
on.emit(this,"valueChanged",{target:this,bubbles:true,cancelable:true});
},_disconnectDownListeners:function(){
for(var i=0;i<this._downListeners.length;i++){
_3.disconnect(this._downListeners[i]);
}
this._downListeners=[];
},_disconnectMoveAndUpListeners:function(){
for(var i=0;i<this._moveAndUpListeners.length;i++){
_3.disconnect(this._moveAndUpListeners[i]);
}
this._moveAndUpListeners=[];
},_disconnectListeners:function(){
this._disconnectDownListeners();
this._disconnectMoveAndUpListeners();
this._disconnectCursorListeners();
},_connectCursorListeners:function(_b){
var _c=_b.connect("onmouseover",this,function(){
this.scale._gauge._setCursor("pointer");
});
this._cursorListeners.push(_c);
_c=_b.connect("onmouseout",this,function(_d){
this.scale._gauge._setCursor("");
});
this._cursorListeners.push(_c);
},_disconnectCursorListeners:function(){
for(var i=0;i<this._cursorListeners.length;i++){
_3.disconnect(this._cursorListeners[i]);
}
this._cursorListeners=[];
},_connectDownListeners:function(){
this._disconnectDownListeners();
this._disconnectCursorListeners();
var _e=null;
var _f;
if(this.interactionMode=="mouse"){
_f="onmousedown";
}else{
if(this.interactionMode=="touch"){
_f="ontouchstart";
}
}
if(this.interactionMode=="mouse"||this.interactionMode=="touch"){
if(this.interactionArea=="indicator"){
_e=this._gfxGroup.connect(_f,this,this._onMouseDown);
this._downListeners.push(_e);
if(this.interactionMode=="mouse"){
this._connectCursorListeners(this._gfxGroup);
}
}else{
if(this.interactionArea=="gauge"){
if(!this.scale||!this.scale._gauge||!this.scale._gauge._gfxGroup){
return true;
}
_e=this.scale._gauge._gfxGroup.connect(_f,this,this._onMouseDown);
this._downListeners.push(_e);
_e=this._gfxGroup.connect(_f,this,this._onMouseDown);
this._downListeners.push(_e);
if(this.interactionMode=="mouse"){
this._connectCursorListeners(this.scale._gauge._gfxGroup);
}
}else{
if(this.interactionArea=="area"){
if(!this.scale||!this.scale._gauge||!this.scale._gauge._baseGroup){
return true;
}
_e=this.scale._gauge._baseGroup.connect(_f,this,this._onMouseDown);
this._downListeners.push(_e);
_e=this._gfxGroup.connect(_f,this,this._onMouseDown);
this._downListeners.push(_e);
if(this.interactionMode=="mouse"){
this._connectCursorListeners(this.scale._gauge._baseGroup);
}
}
}
}
}
return false;
},_connectMoveAndUpListeners:function(){
var _10=null;
var _11;
var _12;
if(this.interactionMode=="mouse"){
_11="onmousemove";
_12="onmouseup";
}else{
if(this.interactionMode=="touch"){
_11="ontouchmove";
_12="ontouchend";
}
}
_10=this.scale._gauge._baseGroup.connect(_11,this,this._onMouseMove);
this._moveAndUpListeners.push(_10);
_10=this._gfxGroup.connect(_11,this,this._onMouseMove);
this._moveAndUpListeners.push(_10);
_10=this.scale._gauge._baseGroup.connect(_12,this,this._onMouseUp);
this._moveAndUpListeners.push(_10);
_10=this._gfxGroup.connect(_12,this,this._onMouseUp);
this._moveAndUpListeners.push(_10);
},_onMouseDown:function(_13){
this._connectMoveAndUpListeners();
this._startEditing();
},_onMouseMove:function(_14){
this._preventAnimation=true;
if(this._animation){
this._animation.stop();
}
},_onMouseUp:function(_15){
this._disconnectMoveAndUpListeners();
this._preventAnimation=false;
this._endEditing();
},_startEditing:function(){
if(!this.scale||!this.scale._gauge){
return;
}else{
this.scale._gauge.onStartEditing({indicator:this});
}
},_endEditing:function(){
if(!this.scale||!this.scale._gauge){
return;
}else{
this.scale._gauge.onEndEditing({indicator:this});
}
}});
});
