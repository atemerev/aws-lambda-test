AWS Lambda test
===============

Hi Hadi and everyone,

Here is a test task implementation according to your spec.

On implementation details:
--------------------------

I am using javax.json (instead of, say, Jackson), because it allows working
with JSON streams. If a streaming API endpoint is used, it will start writing
flattened output immediately, _before_ input is loaded, allowing to work with
huge JSON object. Probably will not work for HTTP endpoint.

Unit tests and API documentation included.