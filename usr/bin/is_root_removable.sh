#!/bin/bash
# A script to test whether a Lernstick root is on a removable media.

function get_root_device() {
  # Extract the root mount info, eg.,
  # overlay on / type overlay (rw,noatime,lowerdir=/live/rootfs/filesystem.squashfs/,upperdir=/live/persistence/sdb2/./rw,workdir=/live/persistence/sdb2/./work)
  local ROOT_LINE=$(mount | grep '^[^ ]\+ on / ')
  # Determine by upperdir only.
  local UPPERDIR=$(echo $ROOT_LINE |
        sed -e 's/^.*upperdir=//' -e 's/,.*$//')
  local DEVICE=$(echo $UPPERDIR |
        sed -e 's,/live/persistence/,,' -e 's,[0-9]\+/.*$,,')
  if [[ "$TEST_DEVICE" != "" ]]; then
    # For testing.
    echo "$TEST_DEVICE"
  else
    echo $DEVICE
  fi
}

DEVICE="$(get_root_device)"
# Probe to determine whether the device is removable.
[[ $(cat /sys/block/${DEVICE}/removable) == "1" ]]
