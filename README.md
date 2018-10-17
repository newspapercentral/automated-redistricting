# automated-redistricting

1. Create the following folder structure
  {Project Folder - any name you want}
      - data
      - jars
      - logs
      - scripts
      - output
2. Download Census Blocks and or Census Tracts into the /data folder
3. Download dac.jar into the /jars folder
      - you can modify the code in this repository and create a new jar if you would like
4. Download dac.sh into the /scripts folder
      - you will want to modify this file to fit your needs
      - currently it runs just NY and FL, but you can remove the if statement if you want to run all of the states
      - MAX_FUNC should be set to the max function (pop, both, contig)
      - UNIT should be set to the data that you selected (tract, block)
      - SWAP should be set to enable or disable population optimization (true, false)
      - SITE should be set to point (this is a potential feature for a future release)
  
 5. Run the code and view the results and log files
