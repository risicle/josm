{
  pkgs ? import <nixpkgs> {}
}:
{
  josmDevEnv = pkgs.stdenv.mkDerivation {
    name = "josm-dev-env";
    buildInputs = [
      pkgs.ant
      pkgs.openjdk
      pkgs.rlwrap  # just try using jdb without it
      pkgs.man
    ];
  };
}
