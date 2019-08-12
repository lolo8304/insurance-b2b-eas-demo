
function GETvar(parameterName) {
    var result = null,
    tmp = [];
    location.search
        .substr(1)
        .split("&")
        .forEach(function (item) {
            tmp = item.split("=");
            if (tmp[0] === parameterName) result = decodeURIComponent(tmp[1]);
    });
    return result;
}

var port = GETvar("port");
var local = GETvar("local");
MAIN_URL="";
if (port != null) {
    if (local != null) {
        MAIN_URL = "http://localhost:"+port
    } else {
        MAIN_URL = document.location.protocol + "//" + document.location.hostname + ":" + port
    }
   //MAIN_URL="http://localhost:"+port;
}

//var toRefresh = true;
var toRefresh = false;
function refreshGrids() {
    if (toRefresh) {
        var grids = [ "#brokerMandatesTemplate", "#insuranceMandatesTemplate", "#offers", "#offeringTemplates"];
        for (grid in grids) {
            $(grids[grid]).jsGrid("reset");
        }
        $( "#runningAnimation" ).hide();
        history.go(0)
    } else {
        timedRefresh(10000);
    }
}
function forceRefreshGrids() {
    toRefresh = true;
    //toRefresh = false;
    refreshGrids();
}

function stopRefresh() {
    toRefresh = false;
    $( "#runningAnimation" ).show();
}

function editTemplateData(self){
    var tokenbefore = $(self).attr("value");
    var token = prompt("Please enter Bearer token", tokenbefore );
    if (token != null && tokenbefore != token) {
        var yourName = prompt("Please enter your name", "" );
        if (yourName != null) {
            stopRefresh();
            $.ajax(
                {
                    url: MAIN_URL+"/api/v1/sidis/eas/patient-records/",
                    method: "PATCH",
                    headers: {
                        "Content-Type" : "application/x-www-form-urlencoded"
                    },
                    data: "data={ \"wallet\" : { \"token\" : \""+token+"\",  \"token-updated-by\" : \""+yourName+"\"} }"
                }
            ).done(function(result) {
                forceRefreshGrids()
            }).fail(function(jqXHR, textStatus) {
                alert(jqXHR.responseText);
                forceRefreshGrids();
            });
        }
    }
}

function saveDummyPatientRecord(self, method){
    if (confirm('Are you sure to initialize patient record?')) {
        stopRefresh();
        $.ajax(
            {
                url: MAIN_URL+"/api/v1/sidis/eas/patient-records/",
                method: method,
                headers: {
                    "Content-Type" : "application/x-www-form-urlencoded"
                },
                data: "data="+getPatientDemo()
            }
        ).done(function(result) {
            forceRefreshGrids()
        }).fail(function(jqXHR, textStatus) {
            alert(jqXHR.responseText);
            forceRefreshGrids();
        });
    }
}



function saveDummyService(self, service, spShort, price, optionalIds){
    stopRefresh();
    $.ajax(
        {
            url: MAIN_URL+"/api/v1/sidis/eas/services/",
            method: "POST",
            headers: {
                "Content-Type" : "application/x-www-form-urlencoded"
            },
            data: "service-name="+encodeURI(service)+"&data="+encodeURI(getServiceData())+"&price="+encodeURI(price)
        }
    ).done(function(result) {
        forceRefreshGrids()
    }).fail(function(jqXHR, textStatus) {
        alert(jqXHR.responseText);
        forceRefreshGrids();
    });
}



function getPatientDemo() {
    return "{\"person\":{\"firstName\":\"John\",\"lastName\":\"Doe\",\"dateOfBirth\":\"1993-09-13\",\"sex\":\"male\"},\"address\":{\"street\":\"NE 29th Place\",\"city\":\"Bellevue\",\"zip\":\"14615\",\"state\":\"WA\",\"country\":\"USA\"},\"communication\":{\"email\":\"john.doe@random.com\",\"phone\":\"(541) 754-3010\",\"mobile\":\"1-541-754-3010\"},\"body-vitals\":{\"bloodType\":\"A+\",\"weight\":\"238 lb\",\"height\":\"6ft 2in\",\"bmi\":\"31.3\",\"bodyFat\":\"0.218\",\"muscleMass\":\"0.25\",\"hipSize\":\"33in\",\"bodyTemperature\":[98],\"heartRate\":[80],\"bloodPressure\":[130],\"respiratoryRate\":[27],\"sleepingBehaviour\":{},\"pedometer/Day\":[6000]},\"nutrition\":{\"foodAllergies\":[\"egg\",\"nuts\"],\"caloriesPerDay\":[2700],\"diets\":[],\"macroPerDay\":[700],\"microPerDay\":[200]},\"allergies\":{\"types\":[\"hayfever\",\"alergic asthma\"]},\"genetics\":{\"investigations\":[\"geneticTest200610\",\"geneticTest151015\"]},\"medical-history\":{},\"medication\":[{\"drugName\":\"Aspirin\",\"isTakenPeriodically\":true}],\"ongoingConditions\":[\"Diabetes\"],\"immunizations\":{\"types\":[\"measles\",\"smallpox\"]},\"wallet\":{\"ethereum\":\"0x049A17DE00c70e7dBfE5A71b8B529D89ce1365Fa\",\"token\":\"d\",\"token-updated-by\":\"Initial\"}}";
}
function getServiceData() {
    return "{\"test\": \"42\"}";
}


function strongS(i) {
    return (i < 10 ? "<strong>" : "");
}
function strongE(i) {
    return (i < 10 ? "</strong><br>" : "");
}


function show_services(tagName, result) {
    var i = 0;
    $(tagName).jsGrid({
        height: "auto",
        width: "100%",

        sorting: true,
        paging: false,
        selecting: false,
        filtering: false,
        autoload: true,

        data: result.reverse(),

        fields: [

            /*
              */
            { title: "Service", name: "serviceName", type: "text", itemTemplate: function(value, item) {
                 i = i + 1;
                 return strongS(i)+item.serviceName +"<br>"+price(item.price) + strongE(i);
                }
            },
            { title: "Roles", name: "", type: "text", width: 150, itemTemplate: function(value, item) {
                 i = i + 1;
                 return strongS(i)+"I: "+X500toO(item.initiatorX500)
                    +"<br>SP: "+X500toO(item.serviceProviderX500)+strongE(i);
                }
            },
            { title: "State", name: "state", type: "text", itemTemplate: function(value, item) {
                 i = i + 1;
                 return strongS(i)+value+strongE(i);
                }
            },
            { title: "Data", name: "serviceData", type: "text", itemTemplate: function(value, item) {
                 i = i + 1;
                var data = Object.keys(value);
                return strongS(i)+data.join("<br>")+strongE(i);
                }
            },
            { title: "Link", name: "id", type: "text", align: "center", width: 30, itemTemplate: function(value) {
                 var res = "<a target='_blank' href='"+MAIN_URL+"/api/v1/sidis/eas/services/"+value.id+"'>o</a>";
                 i = i + 10;
                return strongS(i)+res+strongE(i); }
            }
        ]
    });
}


function X500toOL(x500) {
    if (x500 == null || x500 == "") return "";
    var DNs = x500.split(/[,=]/)
    return DNs[1]+", "+DNs[3]
}
function X500toO(x500) {
    if (x500 == null || x500 == "") return "";
    var DNs = x500.split(/[,=]/)
    return DNs[1];
}

function price(price) {
    if (price == null) return "";
    return "CHF "+price;
}


var ME=""
var ME_brokerMandate=""
var ME_insuranceMandates=[]
var ME_insurers=[]



$(document).ready(function(){

    $.get({
        url: MAIN_URL+"/api/v1/sidis/eas/me",
        data: {        },
        success: function( result ) {
            var x500name = result.me.x500Principal.name.split(",");
            var O=x500name[0].split("=")[1];
            var L=x500name[1].split("=")[1];
            var C=x500name[2].split("=")[1];
            var imageName = O.trim().replace(/[ ]/g, '_').replace(/[,\.]/g, '').toLowerCase();
            $( "#party_me" ).html( O+", "+L+" ("+C+")" );
            $( "#image_me" ).html( "<img style=\"width:80px\" src=\""+imageName+".jpeg\"/>" );
            ME = O;
        }
    }).fail(function(e) {
      $( "#party_me" ).html(e.statusText );
    });


    $.get({
        url: MAIN_URL+"/api/v1/sidis/eas/services/",
        data: { },
        success: function( result ) {
            show_services("#services-template", result);
        }
    });


    timedRefresh(10000);

});

function timedRefresh(timeoutPeriod) {
    setTimeout("refreshGrids();",timeoutPeriod);
}
