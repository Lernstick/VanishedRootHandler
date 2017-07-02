#! /bin/sh

### BEGIN INIT INFO
# Provides:          vanished_root_handler
# Required-Start:    $local_fs $remote_fs
# Required-Stop:
# X-Start-Before:    memlockd
# Default-Start:     2 3 4 5
# Default-Stop:
# Short-Description: Decide vanished_root_handler-specific memlockd config.
# Description:       Decide vanished_root_handler-specific memlockd config.
### END INIT INFO

set -e

SRC_CFG=/usr/share/polkit-1/memlockd/vanished_root_handler.cfg
CFG=/etc/memlockd.d/vanished_root_handler.cfg

case "$1" in
  start)
        if /usr/bin/is_root_removable.sh; then
                cp -f $SRC_CFG $CFG
	else
                rm -f $CFG
        fi
	;;
  stop|reload|restart|force-reload|status)
	;;
  *)
	echo "Usage: $N {start|stop|restart|force-reload|status}" >&2
	exit 1
	;;
esac

exit 0
