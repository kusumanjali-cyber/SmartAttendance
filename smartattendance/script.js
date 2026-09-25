function calculateAttendance() {

    var totalElement = document.getElementById("total");
    var attendedElement = document.getElementById("attended");
    var resultElement = document.getElementById("result");

    var total = Number(totalElement.value);
    var attended = Number(attendedElement.value);

    if (total <= 0) {

        resultElement.innerHTML =
            '<div class="result-box">' +
            '<p>Please enter total classes.</p>' +
            '</div>';

        return false;
    }

    if (attended < 0 || attended > total) {

        resultElement.innerHTML =
            '<div class="result-box">' +
            '<p>Please enter valid attended classes.</p>' +
            '</div>';

        return false;
    }

    var percentage = (attended / total) * 100;

    var message;

    if (percentage >= 75) {
        message = "Your attendance is good.";
    } else {
        message = "Your attendance is below 75%.";
    }

    resultElement.innerHTML =
        '<div class="result-box">' +

        '<h3>Your Attendance</h3>' +

        '<div class="percentage">' +
        percentage.toFixed(2) +
        '%' +
        '</div>' +

        '<p>' +
        attended +
        ' out of ' +
        total +
        ' classes attended.' +
        '</p>' +

        '<p>' +
        message +
        '</p>' +

        '</div>';

    return false;
}


function resetForm() {

    document.getElementById("total").value = "";

    document.getElementById("attended").value = "";

    document.getElementById("result").innerHTML = "";

    return false;
}

