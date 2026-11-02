const handler = require("../../web-dashboard/api/network-data");

exports.handler = async (event) => {
  const responseHeaders = {};
  let statusCode = 200;
  let body = "";

  const req = {
    method: event.httpMethod || "GET",
    headers: event.headers || {},
    query: event.queryStringParameters || {}
  };
  const res = {
    setHeader(name, value) {
      responseHeaders[name] = value;
      return this;
    },
    status(code) {
      statusCode = code;
      return this;
    },
    json(value) {
      body = JSON.stringify(value);
      return this;
    }
  };

  await handler(req, res);
  return {
    statusCode,
    headers: { "Content-Type": "application/json", ...responseHeaders },
    body
  };
};
