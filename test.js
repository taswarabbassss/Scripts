let data = [
    {
        "_id": "5f58b2d7149b3f0006e2e1f8",
        "name": "The Pet Company"
    },
    {
        "_id": "5fa540d4149b3f000624351d",
        "name": "New tenant"
    },
    {
        "_id": "5fa54207149b3f000624351e",
        "name": "ten"
    },
    {
        "_id": "63b872b8d0aa5d204bce4a30",
        "name": "referralTest1"
    },
    {
        "_id": "63b8715ad0aa5d204bce4a2f",
        "name": "referralTest"
    },
    {
        "_id": "5ffc3c02c3698c3bec286f3f",
        "name": "test11"
    },
    {
        "_id": "5f58aaa8149b3f0006e2e1f7",
        "name": "ZTenant"
    },
    {
        "_id": "5ffc22e8c10e025230748508",
        "name": "test3"
    },
    {
        "_id": "5f572b995d15761b68b1ef0c",
        "name": "CRN"
    },
    {
        "_id": "5ffc060a149b3f0006d734aa",
        "name": "test2"
    },
    {
        "_id": "618b710aadbe1d000706aae8",
        "name": "testing123"
    },
    {
        "_id": "5f8e2f15149b3f00067c88fd",
        "name": "mak"
    },
    {
        "_id": "5f605698149b3f0006c1c323",
        "name": "Demo Tenant"
    },
    {
        "_id": "5ffc3833c3698c103c6682b3",
        "name": "test10"
    },
    {
        "_id": "60b730a3a7b11b000a1fe2cc",
        "name": "muneeb"
    },
    {
        "_id": "63b874db1676e173bb3688fc",
        "name": "referralTest2"
    },
    {
        "_id": "63c5b452994d5d0fcea29ba2",
        "name": "referralTest3"
    },
    {
        "_id": "5f68740c149b3f0006b2a540",
        "name": "testTenant"
    },
    {
        "_id": "62610d0dd6b69d5c8e754251",
        "name": "tnt"
    },
    {
        "_id": "5f5a0090149b3f0006f27a7a",
        "name": "ColorTenantr"
    },
    {
        "_id": "648777a3ea59a012a344b1fb",
        "name": "NinePatch"
    },
    {
        "_id": "5f5b6116149b3f0006f27a7b",
        "name": "FGFDG"
    },
    {
        "_id": "5ffc3682c3698c2fb8e23303",
        "name": "test3"
    },
    {
        "_id": "62610bfed6b69d5c8e754250",
        "name": "tnt"
    },
    {
        "_id": "62b981aed82860843a82294a",
        "name": "keycloak"
    },
    {
        "_id": "5ffc2987c10e02557499dd7f",
        "name": "test3"
    },
    {
        "_id": "62bb54f0d0e69041f54c73b2",
        "name": "RIE Demo"
    },
    {
        "_id": "5ffc74dae93c8b44bca1575a",
        "name": "test13"
    },
    {
        "_id": "5ff6d39b149b3f0006d734a9",
        "name": "test"
    },
    {
        "_id": "5ffc336bc10e02557499dd80",
        "name": "test3"
    },
    {
        "_id": "618b6e87adbe1d000706aae7",
        "name": "abced"
    },
    {
        "_id": "5ffc807de93c8b4db4a7473b",
        "name": "test13"
    },
    {
        "_id": "5ff6cd76149b3f0006d734a8",
        "name": "raheel"
    },
    {
        "_id": "5ffc1842149b3f0006d734ab",
        "name": "test4"
    }
]

console.log(data.reduce((accumilator,tenant)=> {
    accumilator[tenant._id] = tenant.name;
    return accumilator;
},{}));