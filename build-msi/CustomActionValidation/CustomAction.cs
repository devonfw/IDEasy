using System.IO;
using System.Linq;
using System.Text.RegularExpressions;
using WixToolset.Dtf.WindowsInstaller;

namespace CustomActionValidation
{
    public class CustomActions
    {
        // Custom action entry point
        [CustomAction]
        public static ActionResult ValidatePath(Session session)
        {
            // Get the installation directory (WIXUI_INSTALLDIR)
            string installFolder = session["INSTALLFOLDER"];

            if (HasUmlauts(installFolder))
            {
                session["INSTALLFOLDER_VALID"] = "0";
                session["VALIDATION_ERROR"] = "Installation path contains Umlauts";
                return ActionResult.Success;
            }
            
            if (installFolder.Length > 32)
            {
                session["INSTALLFOLDER_VALID"] = "0";
                session["VALIDATION_ERROR"] = "The path is too long. Maximum 32 characters";
                return ActionResult.Success;
            }

            if (!Regex.IsMatch(installFolder, @"^[a-zA-Z]:\\([a-zA-Z0-9]+\\)*[a-zA-Z0-9]+\\?$"))
            {
                session["INSTALLFOLDER_VALID"] = "0";
                session["VALIDATION_ERROR"] = "The path contains invalid characters. Only alphanumeric characters are allowed";
                return ActionResult.Success;
            }

            string projectName = "projects";
            string trimmedInstallFolder = installFolder.TrimEnd('\\');
            int lastBackSlashIndex = trimmedInstallFolder.IndexOf('\\');
            string installFolderName = trimmedInstallFolder.Substring(lastBackSlashIndex + 1);
            if (installFolderName != projectName)
            {
                session["INSTALLFOLDER_VALID"] = "0";
                session["VALIDATION_ERROR"] = $"The installation folder must be named \"{projectName}\"";
                return ActionResult.Success;
            }

            // Set again to 1 when path is valid
            session["INSTALLFOLDER_VALID"] = "1";

            return ActionResult.Success;
        }

        private static bool HasUmlauts(string path)
        {
            string umlauts = "äöüÄÖÜß";

            return path.Any(c => umlauts.Contains(c));
        }
    }

}
