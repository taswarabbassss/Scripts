let arr = [0, 1, 0, 0, 1, 0, 0, 0, 1, 1, 1, 0];
let finalData = [];
arr.forEach((item) => {
  if (item === 1) {
    finalData.push(item);
  } else {
    finalData.unshift(item);
  }
});

console.log(finalData);
