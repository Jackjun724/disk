function getLogID(id) {
  var u = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/~！@#￥%……&";
  var d = /[\uD800-\uDBFF][\uDC00-\uDFFFF]|[^\x00-\x7F]/g;
  var f = String.fromCharCode;
  function l(e) {
    if (e.length < 2) {
      var n$__0 = e.charCodeAt(0);
      return 128 > n$__0 ? e : 2048 > n$__0 ? f(192 | n$__0 >>> 6) + f(128 | 63 & n$__0) : f(224 | n$__0 >>> 12 & 15) + f(128 | n$__0 >>> 6 & 63) + f(128 | 63 & n$__0);
    }
    var n = 65536 + 1024 * (e.charCodeAt(0) - 55296) + (e.charCodeAt(1) - 56320);
    return f(240 | n >>> 18 & 7) + f(128 | n >>> 12 & 63) + f(128 | n >>> 6 & 63) + f(128 | 63 & n);
  }
  function g(e) {
    return (e + "" + Math.random()).replace(d, l);
  }
  function m(e) {
    var n = [0, 2, 1][e.length % 3];
    var t = e.charCodeAt(0) << 16 | (e.length > 1 ? e.charCodeAt(1) : 0) << 8 | (e.length > 2 ? e.charCodeAt(2) : 0);
    var o = [u.charAt(t >>> 18), u.charAt(t >>> 12 & 63), n >= 2 ? "=" : u.charAt(t >>> 6 & 63), n >= 1 ? "=" : u.charAt(63 & t)];
    return o.join("");
  }
  function h(e) {
    return e.replace(/[\s\S]{1,3}/g, m);
  }
  function p() {
    return h(g((new Date()).getTime()));
  }
  function w(e, n) {
    return n ? p(String(e)).replace(/[+\/]/g, function(e) {
      return "+" == e ? "-" : "_";
    }).replace(/=/g, "") : p(String(e));
  }
  return w(id);
}