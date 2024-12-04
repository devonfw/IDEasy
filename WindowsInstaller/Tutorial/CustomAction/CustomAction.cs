using Microsoft.Deployment.WindowsInstaller;
using System;
using System.Collections.Generic;
using System.Text;
using System.IO;

namespace CustomAction
{
    public class CustomActions
    {
        [CustomAction]
        public static ActionResult CheckDataDrive(Session session)
        {
            ActionResult retVal;

            session.Log("Begin CheckDataDrive");
            if (TrySetTargetDir("D", session))
            {
                retVal = ActionResult.Success;
            }
            else if (TrySetTargetDir("C", session))
            {
                retVal = ActionResult.Success;
            }
            else
            {
                retVal = ActionResult.Failure;
            }
            session.Log("End CheckDataDrive");

            return retVal;
        }

        private static bool TrySetTargetDir(string driveLetter, Session session)
        {
            var driveInfo = new DriveInfo(driveLetter);
            if (driveInfo.DriveType != DriveType.Fixed || !driveInfo.IsReady)
                return false;

            // Set the INSTALLFOLDER
            session["INSTALLFOLDER"] = $@"{driveLetter}:\Configurations";
            session.Log($"INSTALLFOLDER changed to {session["INSTALLFOLDER"]}");
            return true;
        }
    }
}
