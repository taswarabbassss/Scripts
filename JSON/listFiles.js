const fs = require("fs");
const path = require("path");

// Get the current directory path
const currentDirectory = __dirname;

// Function to get all file names in the current directory
function getFilesInDirectory(directoryPath) {
  fs.readdir(directoryPath, (err, files) => {
    if (err) {
      console.error("Error reading directory:", err);
      return;
    }

    files.forEach((file) => {
      const filePath = path.join(directoryPath, file);
      fs.stat(filePath, (err, stats) => {
        if (err) {
          console.error("Error getting file stats:", err);
          return;
        }

        if (stats.isFile()) {
          console.log(file);
        }
      });
    });
  });
}

// Call the function with the current directory
getFilesInDirectory(currentDirectory);
