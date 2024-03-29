
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
        $( "#runningAnimation" ).hide();
        history.go(0)
    } else {
    }
}
function forceRefreshGrids() {
    toRefresh = true;
    //toRefresh = false;
    refreshGrids();
}

function animationOff() {
    setWebSocketConnected(true, false);
}
function animationOn() {
    setWebSocketConnected(true, true);
}


function createNewService(self, service, spShort, price, optionalIds){
    animationOn();
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

    }).fail(function(jqXHR, textStatus) {
        alert(jqXHR.responseText);
        forceRefreshGrids();
    });
}

function deleteService(service) {
    var id = $(service).attr("value");
    if (confirm('Are you sure to delete service ' + id +'?')) {
        animationOn();
        $.ajax(
            {
                url: MAIN_URL+"/api/v1/sidis/eas/services/"+id,
                method: "DELETE",
                headers: {
                    "Content-Type" : "application/x-www-form-urlencoded"
                },
                data: ""
            }
        ).done(function(result) {
            get_services();
            animationOff();
        }).fail(function(jqXHR, textStatus) {
            alert(jqXHR.responseText);
            forceRefreshGrids();
        });
    }
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
function makeOptions(id, list) {
    var s = "<select id='"+id+"' onChange='onSelectionChanged(this)'><option>choose</option>";
    Object.entries(list).forEach(([key, value]) =>
        s = s + (key == "self" ? "" : "<br><option value=\""+value+"\">"+key+"</option>"));
    s = s + "</select>";
    return s;
}
function onSelectionChanged(select) {
    if ($(select).val() != '') {
            var url = $(select).val();
            var action = url.split("/").reverse()[0];
            if (action != "SHARE") {
                animationOn();
                $.ajax(
                    {
                        url: url,
                        method: "POST",
                        headers: {
                            "Content-Type" : "application/x-www-form-urlencoded"
                        },
                        data: ""
                    }
                ).done(function(result) {

                }).fail(function(jqXHR, textStatus) {
                    alert(jqXHR.responseText);
                    forceRefreshGrids();
                });
            } else {
                animationOn();
                $.ajax(
                    {
                        url: url,
                        method: "POST",
                        headers: {
                            "Content-Type" : "application/x-www-form-urlencoded"
                        },
                        data: "service-provider="+encodeURI(ME_RANDOM_PEER)
                    }
                ).done(function(result) {

                }).fail(function(jqXHR, textStatus) {
                    alert(jqXHR.responseText);
                    forceRefreshGrids();
                });            }

    }
};


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
            { title: "Service", name: "state.serviceName", type: "text", itemTemplate: function(value, item) {
                 i = i + 1;
                 return strongS(i)+item.state.serviceName +"<br>"+price(item.state.price) + strongE(i);
                }
            },
            { title: "Roles", name: "state", type: "text", itemTemplate: function(value, item) {
                 i = i + 1;
                 return strongS(i)+"I: "+X500toO(item.state.initiatorX500)
                    +"<br>SP: "+X500toO(item.state.serviceProviderX500)+strongE(i);
                }
            },
            { title: "State", name: "state.state", type: "text", itemTemplate: function(value, item) {
                 i = i + 1;
                 return strongS(i)+value+"(*)<br>"+makeOptions(item.state.id.id, item.links)+strongE(i);
                }
            },
            { title: "Data", name: "state.serviceData", type: "text", itemTemplate: function(value, item) {
                 i = i + 1;
                var data = Object.keys(value);
                return strongS(i)+data.join("<br>")+strongE(i);
                }
            },
            { title: "Link", name: "state.id", type: "text", align: "center", width: 30, itemTemplate: function(value) {
                 var res = "<a target='_blank' href='"+MAIN_URL+"/api/v1/sidis/eas/services/"+value.id+"'>o</a>&nbsp;"
                    +"<a value="+value.id+" href=\"#\" onClick=\"deleteService(this)\"'>X</a>";
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
var ME_RANDOM_PEER=""
var ME_brokerMandate=""
var ME_insuranceMandates=[]
var ME_insurers=[]

function getRandomInt(max) {
  return Math.floor(Math.random() * Math.floor(max));
}

function get_me() {
    $get({
        url: MAIN_URL+"/api/v1/sidis/eas/me",
        data: {        },
        success: function( result ) {
            var x500name = result.me.x500Principal.name.split(",");
            var O=x500name[0].split("=")[1];
            var L=x500name[1].split("=")[1];
            var C=x500name[2].split("=")[1];
            var imageName = O.trim().replace(/[ ]/g, '_').replace(/[,\.]/g, '').toLowerCase();
            $( "#party_me" ).html( O+", "+L+" ("+C+")" );
            $( "#image_me" ).html( "<img style=\"width:80px\" src=\"images/node_"+imageName+".jpeg\"/>" );
            ME = O;
        }
    }).fail(function(e) {
      $( "#party_me" ).html(e.statusText );
    });
}

function get_peers() {
    $get({
        url: MAIN_URL+"/api/v1/sidis/eas/peers",
        data: {        },
        success: function( result ) {
            ME_RANDOM_PEER = result.peers[getRandomInt(result.peers.length)].x500Principal.name;
        }
    });
}

function get_services() {
    $get({
        url: MAIN_URL+"/api/v1/sidis/eas/services",
        data: { },
        success: function( result ) {
            show_services("#services-template", result);
        }
    });
}

function setWebSocketConnected(connected, running) {
     if (connected && running) {
        $("#image-socket").html("<img id='image-socket-ball' src='images/green.gif'>")
     } else if (connected) {
        $("#image-socket").html("<img id='image-socket-ball' src='images/green.png'>")
     } else {
        $("#image-socket").html("<img id='image-socket-ball' src='images/red.png'>")
     }
}


function connectWebSocket() {
    var socket = new SockJS(MAIN_URL+'/gs-guide-websocket');
    stompClient = Stomp.over(socket);
    stompClient.debug = null;
    stompClient.connect({}, function (frame) {
        setWebSocketConnected(true, false);
        console.log('Connected: ' + frame);
        stompClient.subscribe('/topic/vaultChanged/sidis/eas', function (changes) {
            get_services();
            animationOff();
        });
    });
}


$(document).ready(function(){
    setWebSocketConnected(false);
    get_me();
    get_peers();
    get_services();

    connectWebSocket();

});

function findAPI(url) {
    const regex = /http[s]?\:\/\/[^\/]*\:?[0-9]*(\/.*)/gs;
    let m;

    while ((m = regex.exec(url)) !== null) {
        // This is necessary to avoid infinite loops with zero-width matches
        if (m.index === regex.lastIndex) {
            regex.lastIndex++;
            regex.lastIndex++;
        }

        // The result can be accessed through the `m`-variable.
        m.forEach((match, groupIndex) => {
            return match;
        });
    }
    return url;
}

MOCK = {
    "/api/v1/sidis/eas/services" : [{"state":{"id":{"externalId":null,"id":"3a3fe715-ef9e-4683-a932-7f67b2afbac0"},"state":"SHARED","serviceName":"Tele Medicine","serviceData":{"test":"42"},"price":25,"initiatorX500":"O=AXA Leben AG,L=Winterthur,ST=ZH,C=CH","serviceProviderX500":"O=Swisscanto Pensions Ltd.,L=Zurich,ST=ZH,C=CH","linearId":{"externalId":null,"id":"3a3fe715-ef9e-4683-a932-7f67b2afbac0"}},"links":{"UPDATE":"http://localhost:10804/api/v1/sidis/eas/services/3a3fe715-ef9e-4683-a932-7f67b2afbac0/UPDATE","WITHDRAW":"http://localhost:10804/api/v1/sidis/eas/services/3a3fe715-ef9e-4683-a932-7f67b2afbac0/WITHDRAW","SEND_PAYMENT":"http://localhost:10804/api/v1/sidis/eas/services/3a3fe715-ef9e-4683-a932-7f67b2afbac0/SEND_PAYMENT","ACCEPT":"http://localhost:10804/api/v1/sidis/eas/services/3a3fe715-ef9e-4683-a932-7f67b2afbac0/ACCEPT","DECLINE":"http://localhost:10804/api/v1/sidis/eas/services/3a3fe715-ef9e-4683-a932-7f67b2afbac0/DECLINE","self":"http://localhost:10804/api/v1/sidis/eas/services/3a3fe715-ef9e-4683-a932-7f67b2afbac0"},"error":null},{"state":{"id":{"externalId":null,"id":"fc6f8a5f-dee3-48ad-a245-594e1b631cc9"},"state":"SHARED","serviceName":"New Service","serviceData":{},"price":34,"initiatorX500":"O=Swiss Life Ltd.,L=Zurich,ST=ZH,C=CH","serviceProviderX500":"O=Swisscanto Pensions Ltd.,L=Zurich,ST=ZH,C=CH","linearId":{"externalId":null,"id":"fc6f8a5f-dee3-48ad-a245-594e1b631cc9"}},"links":{"UPDATE":"http://localhost:10804/api/v1/sidis/eas/services/fc6f8a5f-dee3-48ad-a245-594e1b631cc9/UPDATE","WITHDRAW":"http://localhost:10804/api/v1/sidis/eas/services/fc6f8a5f-dee3-48ad-a245-594e1b631cc9/WITHDRAW","SEND_PAYMENT":"http://localhost:10804/api/v1/sidis/eas/services/fc6f8a5f-dee3-48ad-a245-594e1b631cc9/SEND_PAYMENT","ACCEPT":"http://localhost:10804/api/v1/sidis/eas/services/fc6f8a5f-dee3-48ad-a245-594e1b631cc9/ACCEPT","DECLINE":"http://localhost:10804/api/v1/sidis/eas/services/fc6f8a5f-dee3-48ad-a245-594e1b631cc9/DECLINE","self":"http://localhost:10804/api/v1/sidis/eas/services/fc6f8a5f-dee3-48ad-a245-594e1b631cc9"},"error":null}],
    "/api/v1/sidis/eas/me" : {"me":{"commonName":null,"organisationUnit":null,"organisation":"Swisscanto Pensions Ltd.","locality":"Zurich","state":"ZH","country":"CH","x500Principal":{"name":"O=Swisscanto Pensions Ltd.,L=Zurich,ST=ZH,C=CH","encoded":"ME4xCzAJBgNVBAYTAkNIMQswCQYDVQQIDAJaSDEPMA0GA1UEBwwGWnVyaWNoMSEwHwYDVQQKDBhTd2lzc2NhbnRvIFBlbnNpb25zIEx0ZC4="}}},
    "/api/v1/sidis/eas/peers" : {"peers":[{"commonName":null,"organisationUnit":null,"organisation":"Swiss Life Ltd.","locality":"Zurich","state":"ZH","country":"CH","x500Principal":{"name":"O=Swiss Life Ltd.,L=Zurich,ST=ZH,C=CH","encoded":"MEUxCzAJBgNVBAYTAkNIMQswCQYDVQQIDAJaSDEPMA0GA1UEBwwGWnVyaWNoMRgwFgYDVQQKDA9Td2lzcyBMaWZlIEx0ZC4="}},{"commonName":null,"organisationUnit":null,"organisation":"AXA Leben AG","locality":"Winterthur","state":"ZH","country":"CH","x500Principal":{"name":"O=AXA Leben AG,L=Winterthur,ST=ZH,C=CH","encoded":"MEYxCzAJBgNVBAYTAkNIMQswCQYDVQQIDAJaSDETMBEGA1UEBwwKV2ludGVydGh1cjEVMBMGA1UECgwMQVhBIExlYmVuIEFH"}},{"commonName":null,"organisationUnit":null,"organisation":"FZL","locality":"Zug","state":"ZG","country":"CH","x500Principal":{"name":"O=FZL,L=Zug,ST=ZG,C=CH","encoded":"MDYxCzAJBgNVBAYTAkNIMQswCQYDVQQIDAJaRzEMMAoGA1UEBwwDWnVnMQwwCgYDVQQKDANGWkw="}}]}
}

function API_Failed() {
    this.f;
}

API_Failed.prototype.fail = function(f) {
    this.f = f;
}

jQuery.fn.fail = function(f) {
    var o = $(this[0]) // This is the element
    f("missing mock for "+o);
    return this; // This is needed so other functions can keep chaining off of this
};



function $get(object) {
    if (local == "true") {
        var api = findAPI(object.url);
        if (api && MOCK[api]) {
            console.log("successful MOCK for url "+object.url);
            console.log(MOCK[api]);
            object.success(MOCK[api]);
        } else {
            console.log("missing MOCK for url "+object.url);
        }
        return $(API_Failed());
    }
    return $.get(object);
}
