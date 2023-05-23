# [MITS Container Service](https://its.umich.edu/computing/virtualization-cloud/container-service/)
## k8s folder
### deployment.yml
This file will deploy the dspace *demo* application and its supporting services.
## Deploy to [Red Hat OpenShift Service on AWS](https://containers.aws.web.umich.edu/)
### +Add Deployment
From the `Developer` view click `+Add` in the left menu and select `Import YAML` under `From Local Machine`. Copy and paste the contents of `deployment.yml` and click the `Create` button.
### Create Routes
From the `Administrator` view under `Networking` click `Routes`. Click the `Create Route` button to add routes to the service `dspace`.
### Verify Deployment
From the `Developer` view click `Topology`. Select the `app` which is listed under `Deployment` which should reveal a right side panel. In the right click the links under `Routes` to verify archivesspace is running.
