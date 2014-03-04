The sample which treats to Websocket the data which carried out Base64+ZIP compression by PlayFramework 

1. $ git clone https://github.com/anagotan/play_websocket_compressed.git 
2. $ cd play_websocket_compressed
3. $ play run
4. $ redis-server &
5. browser accesss to "http://localhost:9000/board?code=aaaaa"
6. $ redis-cli  publish "aaaaa" "{\"data\":\"abcdefghijkl\"}"
7. show "abcdefghijkl" in browser

