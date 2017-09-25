#!/bin/bash
set -e

#export MAVEN_OPTS="-ea"
scriptdir=$(cd $(dirname $0); pwd -P)
cli=$scriptdir/btc-scripts/cli.sh
btc_env=regtest
privkey=
datadir=$btc_env-server
conffile=$datadir/config.ini
start_bitcoind=0
bitcoinddir=
txid=

# parse command line args (first colon (:) disables verbose mode, second colon means the option requires an argument)
# (see more info here about getopts: http://wiki.bash-hackers.org/howto/getopts_tutorial)
while getopts ":hd:bt:n:s:" opt; do
    case $opt in
        d)
            datadir=$OPTARG
            ;;
        b)
            start_bitcoind=1
            ;;
        t)
            txid=$OPTARG
            ;;
        n)
            btc_env=$OPTARG
            datadir=$btc_env-server
            ;;
        s)
            privkey=$OPTARG
            ;;
        h)
            echo "Usage: $0 [OPTIONS]"
            echo
            echo "OPTIONS:"
            echo
            echo "  -d <datadir>   stores the Catena server's data (blockchain, wallet) in the specified"
            echo "                 directory (default: $datadir)"
            echo
            echo "  -t <txid>      the root-of-trust TXID, if any, needed when restarting an"
            echo "                 already-initialized Catena server"
            echo
            echo "  -n <btcnet>    the Bitcoin network you are running on: mainnet, testnet or regtest"
            echo "                 (default: $btc_env)"
            echo
            echo "  -s <priv-key>  the private key of the Catena chain, in WIF format, used for signing"
            echo "                 statements"
            echo 
            echo "  -b             starts a bitcoind regtest daemon in the specified directory"
            echo "                 and generates funds for the Catena server, storing the private"
            echo "                 key in the '$conffile' config file (default: no)"
            exit 0
            ;;
        \?)
            echo "Invalid option: -$OPTARG" >&2
            exit 1
            ;;
        :)
            echo "Option -$OPTARG requires an argument." >&2
            exit 1
            ;;
    esac
done
 
# shift away the processed 'getopts' args
shift $((OPTIND-1))
# rest of 'mass-arguments' or 'operands', if needed, are in $@

# store bitcoind's data close by, if we're gonna launch it
conffile=$datadir/config.ini

# if we should start a bitcoind instance
if [ $start_bitcoind -eq 1 ]; then
    if [ "$btc_env" == "regtest" ]; then
        bitcoinddir=$datadir/btcd

        # if this is the first time we're launching bitcoind in that directory, then initialize the chain and get Catena funds
        if [ ! -d "$bitcoinddir" ]; then
            if [ -f "$conffile" ]; then
                echo "ERROR: Cannot start bitcoind and overwrite previous config file '$conffile'. Please move it and try again."
                exit 1
            fi

            # also generates 101 blocks to make the first coinbase TX spendable
            $scriptdir/btc-scripts/start-bitcoind.sh -d "$bitcoinddir"
            # generates the 101 blocks (don't use -g in start-bitcoind.sh because blocks won't be generated by the time it returns)
            BTC_CLI_EXTRA_ARGS='-rpcwait' $scriptdir/btc-scripts/gen-101-blocks.sh
            # creates a private key for us with some coins
            privkey=`BTC_CLI_EXTRA_ARGS='-rpcwait' $scriptdir/btc-scripts/get-catena-funds.sh`

            # create the config file for the Catena server
            echo "privkey=$privkey" >$conffile
            echo "btc_env=$btc_env" >>$conffile
        else
            if ! $scriptdir/btc-scripts/is-bitcoind-running.sh "$bitcoinddir/regtest" &>/dev/null; then
                $scriptdir/btc-scripts/start-bitcoind.sh -d "$bitcoinddir"
            else
                echo "bitcoind is already running in $bitcoinddir, no need to launch it"
            fi
        fi
    elif [ "$btc_env" == "testnet" ]; then
        bitcoinddir=$scriptdir/testnet
        btcd_first_run=0
        btcd_running=0
        if [ -d "$bitcoinddir" ]; then
            if $scriptdir/btc-scripts/is-bitcoind-running.sh $bitcoinddir/testnet3 &>/dev/null; then
                echo "bitcoind testnet instance is already running in $bitcoinddir"
                btcd_running=1
            else
                echo "bitcoind testnet instance is not running in $bitcoinddir"
            fi
        else
            echo "this is the first time you are launching the bitcoind testnet instance"
            btcd_first_run=1
        fi

        if [ $btcd_running -eq 0 ]; then
            # launch bitcoind testnet instance
            $scriptdir/btc-scripts/start-bitcoind.sh -d -t "$bitcoinddir"
        fi
            
        if [ $btcd_first_run -eq 1 ]; then
            export BTC_CLI_EXTRA_ARGS='-rpcwait'
            if [ -z "$privkey" ]; then
                # generate a new address for the Catena account
                addr=`$cli getaccountaddress catena`
                echo "Catena chain address: $addr" 1>&2

                # dump the private key of the Catena account to a file so the Catena server can use it
                privkey=`$cli dumpprivkey $addr`
            else
                # import the specified private key in the wallet
                $cli importprivkey $privkey
            fi

            # create the config file for the Catena server
            echo "privkey=$privkey" >$conffile
            echo "btc_env=$btc_env" >>$conffile

            # log some info for the user
            echo "Catena chain private key: $privkey" 1>&2
        fi
    elif [ "$btc_env" == "mainnet" ]; then
        echo "ERROR: Not implemented yet."
        exit 1
    else
        echo "ERROR: Unsupported network '$btc_env.' Please try one of regtest, testnet or mainnet."
        exit 1
    fi
fi

## Example config file
#privkey=...
#txid=...
#btc_env=mainnet

if [ ! -f "$conffile" ]; then
    echo "ERROR: No '$conffile' config file found, please create one."
    exit 1
else
    :
fi

# source the config file directly
. $conffile

if [ -z "$privkey" ]; then
    echo "ERROR: You must specify the private key of the Catena chain using -s <priv-key> or in the config file $conffile using privkey=<priv-key>"
    exit 1
fi

# if no *.class files, then compile
if [ ! -d $scriptdir/target/ ]; then
    echo
    echo "Compiling..."
    echo
    mvn compile
fi

echo
echo "Running Catena server with: "
echo " * directory: $datadir"

if [ $start_bitcoind -eq 1 ]; then
    echo " * with bitcoind instance: $bitcoinddir"
else
    echo " * no bitcoind instance"
fi 

if [ -n "$txid" ]; then
    echo " * root-of-trust TXID: $txid"
else
    echo " * no root-of-trust TXID"
fi

echo " * private key: $privkey"
echo " * btc net: $btc_env"
[ -n "$*" ] && echo " * extra args: $*"
echo
(cd $scriptdir/;
mvn exec:java -Dexec.mainClass=org.keybase.NotaryApp -Dexec.cleanupDaemonThreads=false -Dexec.args="$privkey $btc_env $datadir $txid $*")
echo
