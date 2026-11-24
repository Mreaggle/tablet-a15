const categories = document.querySelectorAll(".categories > a");
const options = document.getElementById("optionsUl");
const appliedOptions = document.getElementById("appliedOptions");
const appliedOptionsOl = document.getElementById("appliedOptionsOl");
const dollsUl = document.getElementById("dollsUl");
const canvas = document.getElementById("canvas");
const toggleMultiselectBtn = document.getElementById("toggleMultiselectBtn");
const switchDollBtn = document.getElementById("switchDollBtn");
const saveDollsBtn = document.getElementById("saveDollsBtn");
const createNewDollCard = document.getElementById("createNewDollCard");
const deleteAllDollsCard = document.getElementById("deleteAllDollsCard");
var modal = document.getElementById("modal");

var currentCategory;
var toggleMultiselect;
var dolls = [];
var currentDoll;

// fetch('./file.txt')
//   .then(response => {
//     console.log(response)
//     if (response.ok) {
//       return response.text()
//     } else {
//       throw new Error("File not found")
//     }
//   })
//   .then(text => console.log(text.split("\r\n")))
//   .catch(err => console.log(err))

// while()
class Doll {
  constructor() {
    this.options = [];
    var bodyImg = new Image();
    bodyImg.src = "./images/body/0.png";
    this.body = bodyImg;
    this.dataURL = "";
    var doll = this;
    [["Dress", 2], ["Face", 0], ["Hair", 1]].forEach(function (item) {
      var category = item[0];
      var image = new Image();
      image.onload = function () { if (currentDoll === doll) doll.draw(); };
      image.src = "./images/" + category.toLowerCase() + "/" + item[1] + ".png";
      doll.options.push(new Option(category, image));
    });
    bodyImg.onload = function () { if (currentDoll === doll) doll.draw(); };
  }

  setFromJsonData(json) {
    Object.assign(this, json);

    //clear and set options
    this.options = [];
    json.options.forEach((opt) => {
      var image = new Image();
      image.src = opt.image;

      var rebuiltOption = new Option(opt.category, image);
      this.options.push(rebuiltOption);
    });

    //set body
    var bodyImg = new Image();
    bodyImg.src = "./images/body/0.png";
    this.body = bodyImg;
  }

  addOption(option) {
    this.options.push(option);

    renderAppliedOptionsList();
  }

  hasOption(category, image) {
    var dollHasOption = false;

    this.options.forEach((opt) => {
      if (opt.category == category && opt.image.src == image.src) {
        dollHasOption = true;
      }
    });

    return dollHasOption;
  }

  removeOption(option) {
    this.options.forEach((opt, index) => {
      if (
        opt.image.src == option.image.src &&
        opt.category == option.category
      ) {
        this.options.splice(index, 1);
      }
    });

    renderAppliedOptionsList();
  }

  removeAllOptionsFromCategory(category) {
    //we have to loop backwards here and splice when there is a match or else not all options will be reached
    var i;
    i = this.options.length;
    while (i--) {
      if (this.options[i].category == category) {
        this.options.splice(i, 1);
      }
    }

    renderAppliedOptionsList();
  }

  setDataURL(url) {
    this.dataURL = url;
  }

  // draw() will loop through all the options in this doll object and place them all on the canvas
  draw() {
    const ctx = canvas.getContext("2d");
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    if (this.body.naturalWidth) ctx.drawImage(this.body, 50, 20, 250, 500);

    this.options.forEach((opt) => {
      if (opt.image.naturalWidth) ctx.drawImage(opt.image, 50, 20, 250, 500);
    });
  }
}

class Option {
  constructor(category, image) {
    this.category = category;
    this.image = image;
  }

  setFromJsonData(json) {
    Object.assign(this, json);
  }

  toJSON() {
    return {
      category: this.category,
      image: this.image.src,
    };
  }
}

//Load old data or start fresh
if (localStorage.getItem("dolls")) {
  dollsJson = JSON.parse(localStorage.getItem("dolls"));

  dollsJson.forEach((d) => {
    rebuiltDoll = new Doll();
    rebuiltDoll.setFromJsonData(d);
    dolls.push(rebuiltDoll);
  });
  modal.style.display = "block";

  loadDolls();
} else {
  //No existing dolls => create a new doll
  currentDoll = new Doll();
  dolls.push(currentDoll);
}

window.addEventListener("load", () => {
  // resize the canvas: when small screen enlarge the height so the legs fit the canvas
  // if (window.innerHeight < 600) {
  //   canvas.height = window.innerHeight * 0.85;
  // } else {
  //   canvas.height = window.innerHeight * 0.60;
  // }
  canvas.height = "500";
  canvas.width = "400";

  if (currentDoll) currentDoll.draw();
});

// when resizing the browser window, resize the canvas
window.addEventListener("resize", () => {
  // if (window.innerHeight < 750) {
  //   canvas.height = window.innerHeight * 0.85;
  // } else {
  //   canvas.height = window.innerHeight * 0.60;
  // }
  // if (window.innerHeight * 0.85 > 500) {
  //   canvas.height = "500";
  // } else {
  //   canvas.height = window.innerHeight * 0.85;
  // }

  canvas.height = "500";
  canvas.width = "400";
  if (currentDoll) currentDoll.draw();
});

//Create all the categories, when a category is clicked dynamically create all the corresponding options
//by loading images from the current category folder and checking if the image exists
//if the image exists, create a new option li
categories.forEach((cat, index) => {
  cat.addEventListener("click", (cat) => {
    var categoryButton = cat.currentTarget;
    //add animation
    categoryButton.style.transform = "scale(1.05)";
    setTimeout(() => {
      categoryButton.style.transform = "scale(1)";
    }, 200);
    // make sure my options element is visisble again
    options.parentElement.style.visibility = "visible";

    // remove clickedCategory on each item, and add on the last clicked item
    categories.forEach((category) => {
      category.classList.remove("clickedCategory");
    });
    categoryButton.classList.add("clickedCategory");
    currentCategory = categoryButton.dataset.category;
    options.dataset.category = currentCategory;

    // this will empty my options so previous loaded options will be removed
    options.innerHTML = "";

    var category = currentCategory;
    for (let index = 0; index < 10; index++) {
      const tile = document.createElement("li");
      const image = new Image();
      image.draggable = false;
      image.alt = "";
      tile.hidden = true;
      tile.appendChild(image);
      options.appendChild(tile);
      image.onload = function () {
        if (currentDoll.hasOption(category, image)) tile.classList.add("clickedOption");
        tile.hidden = false;
      };
      image.onerror = function () { tile.remove(); };
      image.src = "./images/" + category.toLowerCase() + "/" + index + ".png";
      tile.addEventListener("click", function () {
        const item = new Option(category, image);
        if (currentDoll.hasOption(category, image)) {
          currentDoll.removeOption(item);
          tile.classList.remove("clickedOption");
        } else {
          if (!toggleMultiselect) {
            currentDoll.removeAllOptionsFromCategory(category);
            options.querySelectorAll(".clickedOption").forEach(function (selected) {
              selected.classList.remove("clickedOption");
            });
          }
          currentDoll.addOption(item);
          tile.classList.add("clickedOption");
        }
        currentDoll.draw();
      });
    }
  });

  //Automatically click on the first category so this category options is shown on screen
  if (index == 0) {
    cat.click();
  }
});

toggleMultiselectBtn.addEventListener("click", () => {
  //add animation
  toggleMultiselectBtn.style.transform = "scale(1.05)";
  setTimeout(() => {
    toggleMultiselectBtn.style.transform = "scale(1)";
  }, 200);
  toggleMultiselectBtn.classList.toggle("active");

  if (toggleMultiselectBtn.classList.contains("active")) {
    toggleMultiselect = true;
  } else {
    toggleMultiselect = false;

    //clear all the previous selected elements
    //remove all the previous options in the ui
    var optionsArray = [...options.children];
    optionsArray.forEach((option) => {
      option.classList.remove("clickedOption");
    });

    //remove all the previous options in the doll object
    currentDoll.removeAllOptionsFromCategory(currentCategory);

    currentDoll.draw();
  }
});

switchDollBtn.addEventListener("click", () => {
  //export current doll canvas to a dataURL to display
  currentDoll.setDataURL(canvas.toDataURL("png", 1.0));

  // Show modal
  modal.style.display = "block";

  //clear earlier dolls
  removeDollsUlChildren();

  //Load the existing dolls in the dollsUl
  loadDolls();
});

function loadDolls() {
  dolls.forEach((d, index) => {
    var dollOption = document.createElement("li");
    var image = document.createElement("img");
    image.src = d.dataURL;

    dollOption.appendChild(image);

    dollOption.addEventListener("click", (clickedDoll) => {
      //add animation
      dollOption.style.transform = "scale(1.05)";
      setTimeout(() => {
        dollOption.style.transform = "scale(1)";
      }, 200);
      setTimeout(() => {
        // Close modal
        modal.style.display = "none";
      }, 400);

      //switch doll
      currentDoll = dolls[index];

      //re draw the current doll
      currentDoll.draw();

      //clear the selected options in the ui and select the options in the currentdoll
      var optionsArray = [...options.children];
      optionsArray.forEach((opt) => {
        if (currentDoll.hasOption(currentCategory, opt.firstChild)) {
          opt.classList.add("clickedOption");
        } else {
          opt.classList.remove("clickedOption");
        }
      });

      //clear the applied options
      renderAppliedOptionsList()
    });

    dollsUl.appendChild(dollOption);
  });
}

saveDollsBtn.addEventListener("click", () => {
  localStorage.setItem("dolls", JSON.stringify(dolls));

  saveDollsBtn.innerText = "Bonecas salvas!";

  setTimeout(() => {
    saveDollsBtn.innerText = "Salvar bonecas";
  }, 3000);
});

function removeDollsUlChildren() {
  while (dollsUl.childElementCount > 2) {
    dollsUl.removeChild(dollsUl.lastChild);
  }
}

createNewDollCard.addEventListener("click", () => {
  // Close modal
  modal.style.display = "none";

  currentDoll = new Doll();
  dolls.push(currentDoll);

  //re draw the current doll
  currentDoll.draw();

  //clear the selected options in the ui
  var optionsArray = [...options.children];
  optionsArray.forEach((opt) => {
    opt.classList.remove("clickedOption");
  });

  //clear the applied options
  renderAppliedOptionsList()
});

deleteAllDollsCard.addEventListener("click", () => {
  localStorage.removeItem("dolls");

  //clear screen
  dolls = [];
  currentDoll = null;

  //remove all children except first two: 'delete all dolls' and 'create new doll'
  removeDollsUlChildren();
});

// appliedOptions draggable list
let dragStartIndex;
var listItems = [];

function renderAppliedOptionsList() {
  appliedOptionsOl.innerHTML = "";
  listItems = [];


  currentDoll.options.forEach((option2, index) => {
    var imageClone = option2.image.cloneNode(true);
    imageClone.draggable = false;
    var li = document.createElement('li')
    li.classList.add('appliedOption');
    li.classList.add('draggable');
    li.draggable = true;
    li.setAttribute('data-index', index);
    li.appendChild(imageClone)

    appliedOptionsOl.appendChild(li)
    listItems.push(li);
  })
  addEventListeners();
}

function addEventListeners() {
  const draggables = document.querySelectorAll('.draggable');
  const draggableListItems = document.querySelectorAll('#appliedOptionsOl li');

  draggables.forEach(draggable => {
    draggable.addEventListener('dragstart', dragStart);

  })
  draggableListItems.forEach(draggableListItem => {
    draggableListItem.addEventListener('dragover', dragOver);
    draggableListItem.addEventListener('drop', dragDrop);
    draggableListItem.addEventListener('dragenter', dragEnter);
    draggableListItem.addEventListener('dragleave', dragLeave);
  })
}

function dragStart() {
  dragStartIndex = +this.closest('li').getAttribute('data-index');
}
function dragDrop() {
  const dragEndIndex = +this.getAttribute('data-index');
  swapItems(dragStartIndex, dragEndIndex);

  this.classList.remove('dragover')
}
function dragOver(e) {
  e.preventDefault();
}
function dragEnter() {
  this.classList.add('dragover');
}
function dragLeave() {
  this.classList.remove('dragover');
}

function swapItems(fromIndex, toIndex) {
  //swap items in the doll
  const item = currentDoll.options[toIndex]
  currentDoll.options[toIndex] = currentDoll.options[fromIndex]
  currentDoll.options[fromIndex] = item

  //re-draw the newly ordered currentdoll options
  currentDoll.draw();
  //re-render the currentdoll options in the draggable list
  //renderAppliedOptionsList()
  //OR
  //swap the html items and don't rerender
  const item1 = listItems[fromIndex].querySelector('img')
  const item2 = listItems[toIndex].querySelector('img')
  listItems[toIndex].appendChild(item1)
  listItems[fromIndex].appendChild(item2)


}