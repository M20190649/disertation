config = {
    protocol: "http",
    host: "mycitydashboard.com",
    port: 8080,
    fbAppId: '633618420074584',
    get uri() {
        return this.protocol + "://" + this.host + (this.port ? (":" + this.port): "");
    },
    get accessToken() {
        return storage.accessToken;
    }
}