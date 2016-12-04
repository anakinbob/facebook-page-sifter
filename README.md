## Facebook Page Sifter
This is an app that can sift through the posts on a facebook page/group and filter based on search terms, and output to a file.

## How to Use:
1. Clone the project and open in a Java IDE that supports maven
2. Install maven dependencies
3. Navigate to src/main/resources/params.json. Make sure to fill in the value for the facebook auth token. One can be generated here: https://developers.facebook.com/tools/explorer/
4. Replace the group_id value with one for a page/group you want. To find it simply insert it in a facebook ID finder website such as this: http://findfacebookid.com/
5. Make sure to replace earliest_date with the earliest date you want posts to be included. Format is yyyy-mm-dd
6. Change other params as necessary and run the project