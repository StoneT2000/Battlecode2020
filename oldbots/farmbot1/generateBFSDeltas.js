function calculateManhattanDeltas(dist) {
  var manD = [[0,0]];
  function inDist(x,y) {
    if (x*x + y*y <= dist) {
    return true;
    }
    return false;
  }
  for (let k = 0; k <= dist; k++){
    for (let i = k; i >= 1; i--) {
    if(inDist(i, -k+i))
        manD.push([i, -k + i]);
    }
    for (let i = 0; i >= -k + 1; i--) {
    if(inDist(i, -k-i))
     manD.push([i, -k - i]);
    }
    for (let i = -k; i <= -1; i++) {
    if(inDist(i, k+i))
     manD.push([i, k + i]);
    }
    for (let i = 0; i <= k - 1; i++) {
    if(inDist(i, k-i))
     manD.push([i, k - i]);
    }
  }
  return manD;
}
function turnToJavaArray(arr) {
    let str = "";
    for (let i = 0; i < arr.length; i++) {
        let k = "{" + arr[i].toString() + "}";
        str+=k;
        if (i < arr.length - 1) {
            str += ",";
        }
    }
    return "{" + str + "}";
}
turnToJavaArray(calculateManhattanDeltas(35));