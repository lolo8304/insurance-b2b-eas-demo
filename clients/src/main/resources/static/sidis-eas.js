
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
    var insurer = "O=AXA Leben AG,L=Winterthur,ST=ZH,C=CH";
    var serviceProvider = (spShort == "B") ? "O=FZL,L=Zug,ST=ZG,C=CH" : "O=Swisscanto Pensions Ltd.,L=Zurich,ST=ZH,C=CH";
    if (price == 0) {
        var priceString = prompt("Enter a price", "12" );
        var p = parseInt(priceString, 10);
        if (p > 0) {
            price = p;
        } else {
            return;
        }
    }
    stopRefresh();
    $.ajax(
        {
            url: MAIN_URL+"/api/v1/sidis/eas/share-data/",
            method: "POST",
            headers: {
                "Content-Type" : "application/x-www-form-urlencoded"
            },
            data: "service-name="+encodeURI(service)+"&insurance="+encodeURI(insurer)+"&service-provider="+encodeURI(serviceProvider)+"&mandatory-ids=person&optional-ids="+encodeURI(optionalIds)+"&additional-data="+encodeURI("{}")+"&price="+price
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



function show_patient_records(tagName, result) {

    if (ME == "John Doe") {
        if (result != null && result.length < 2) {
            var method = result.length == 0 ? "POST" : "PUT";
            $( "#patient-record-new" ).html(
                " <a id='patient-dummy' href='#' onClick='saveDummyPatientRecord(this, \""+method+"\")'>init record</a>"
            );
            $( "#patient-record-new" ).css({"display": "block"});
        }
     } else {
            $( "#patient-record_block" ).css({"display": "none"});
     }

    $(tagName).jsGrid({
        height: "auto",
        width: "100%",

        sorting: true,
        paging: false,
        selecting: false,
        filtering: false,
        autoload: true,

        data: result,

        fields: [

            /*
               person: {
                firstName: "John",
                lastName: "Doe",
                dateOfBirth: "1993-09-13",
                sex: "male"

                address
                    street: "NE 29th Place",
                   city: "Bellevue",
                   zip: "14615",
                   state: "WA",
                   country: "USA" "

                   */

            /*
            body stats

            body-vitals: {
               bloodType: "A+",
               weight: "238 lb",
               height: "6ft 2in", "/

              /*
              nutrition: {
              foodAllergies: [
              "egg",
              "nuts"
              ],



              body condition

              allergies: {
              types: [
              "hayfever",
              "alergic asthma"
              ]

              medication: [
              {
              drugName: "Aspirin",
              isTakenPeriodically: true
              }

              immunizations: {
                types: [
                "measles",
                "smallpox"
                ]

              */
            { title: "Person data", name: "dataObject", type: "text", itemTemplate: function(value, item) {
                var p = value.person;
                var a = value.address;
                 return p.firstName+" "+p.lastName+", "+p.dateOfBirth+", "+p.sex
                    + "<br><br>"
                    + a.street+", "+a.city+" "+a.state+", "+a.zip+", "+a.country;
                }
            },
            { title: "body stats", name: "dataObject", type: "text", itemTemplate: function(value, item) {
                var vitals = value["body-vitals"] || value["body vitals"] ;
                var nutrition = value["nutrition"];
                 return vitals.bloodType+"<br>"+vitals.weight+", "+vitals.height
                    + "<br><br>"
                    + "foodAllergies: "+nutrition.foodAllergies.join(", ");
                }
            },
            { title: "body condition", name: "dataObject", type: "text", itemTemplate: function(value, item) {
                 return "allergies: "+value.allergies.types.join(", ")
                    + "<br><br>"
                    + "medication: "+value.medication.map(x => x.drugName).join(", ")
                    + "<br><br>"
                    + "immunizations: "+value.immunizations.types.join(", ");
                }
            },
            { title: "Link", name: "id", type: "text", align: "center", width: 30, itemTemplate: function(value, item) {
                var link1 = "<a target='_blank' href='"+MAIN_URL+"/api/v1/sidis/eas/patient-records/"+value.id+"'>o</a>";
                var json = item.dataObject.wallet.token;
                return link1 +
                    " <a id='token' value='"+json+"' href='#' onClick='editTemplateData(this)'>t</a>";
               }
            }
        ]
    });
}

function strongS(i) {
    return (i < 10 ? "<strong>" : "");
}
function strongE(i) {
    return (i < 10 ? "</strong><br>" : "");
}


function show_shared_data(tagName, result) {
    if (ME == "John Doe") {
        $( "#share-data-new" ).css({"display": "block"});
     }

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
                 return strongS(i)+item.serviceName +"<br>$ "+item.price+strongE(i);
                }
            },
            { title: "Roles", name: "", type: "text", width: 150, itemTemplate: function(value, item) {
                 i = i + 1;
                 return strongS(i)+"P: "+X500toO(item.patientX500)
                    +"<br>I: "+X500toO(item.insurerX500)
                    +"<br>SP: "+X500toO(item.serviceProviderX500)+strongE(i);
                }
            },
            { title: "permissions", name: "mandatoryData", type: "text", itemTemplate: function(value, item) {
                 i = i + 1;
                var mandatory = Object.keys(item.mandatoryData).map(x => x+"(*)");
                var optional = Object.keys(item.optionalData);
                var permission = [...mandatory, ...optional];
                return strongS(i)+permission.join("<br>")+strongE(i);
                }
            },
            { title: "Link", name: "id", type: "text", align: "center", width: 30, itemTemplate: function(value) {
                 var res = "<a target='_blank' href='"+MAIN_URL+"/api/v1/sidis/eas/share-data/"+value.id+"'>o</a>";
                 i = i + 10;
                return strongS(i)+res+strongE(i); }
            }
        ]
    });
}


function X500toOL(x500) {
    var DNs = x500.split(/[,=]/)
    return DNs[1]+", "+DNs[3]
}
function X500toO(x500) {
    var DNs = x500.split(/[,=]/)
    return DNs[1];
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
        url: MAIN_URL+"/api/v1/sidis/eas/patient-records/",
        data: { },
        success: function( result ) {
            show_patient_records("#patient-record-template", result);
        }
    });

    $.get({
        url: MAIN_URL+"/api/v1/sidis/eas/share-data/",
        data: { },
        success: function( result ) {
            show_shared_data("#share-data-template", result);
        }
    });


    timedRefresh(10000);

});

function timedRefresh(timeoutPeriod) {
    setTimeout("refreshGrids();",timeoutPeriod);
}
