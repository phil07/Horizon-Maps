# Horizon Nav: to-do list

Rebuilt on 29 October 2026 from the Reddit threads (r/Androidheadunits and r/AndroidAuto), GitHub issues and messages passed on directly, and checked against the code. The numbers are new; they don't match the older list.

Move items to **Done** as they ship, and add new requests at the bottom of the right list.

## Bugs still open

1. **The whole head unit freezes and restarts on a built-in Android 9 unit (Snapdragon 8155).** *Pending.* The app now spots the unit restarting and steps down to safer drawing. The first step draws the map in the app's own window instead of straight onto the screen. The next feedback report from that unit will show memory and what happened just before the restart. The user has also been asked to try turning off Fast map drawing.

## Features requested

6. **Import saved places from Google Maps** (from a Google Takeout file). *Skipped for now (owner's decision, 2 Nov 2026).*
7. **Copy offline map packs from another device.** For example, download on a phone or PC at home and copy them across by USB, for when the car can't reach home Wi-Fi. *Not needed (owner's decision, 2 Nov 2026).*
8. **Smaller offline packs**, such as Singapore on its own instead of Singapore, Malaysia and Brunei together, and town names for every country in a shared pack.
9. **Swap the search bar for a search icon**, for people who want even more of the map on show.
10. **Choose where your arrow sits on screen.** Requested on Android Auto, for one car where the centre felt too far left. The automatic centring added since may cover this. Check before building.
11. **Radarbot integration** for speed cameras on the route. Radarbot doesn't offer a public way in, so this may not be possible. The app's own camera data is used instead.
13. **Android Auto voice search.** Pressing Android Auto's own mic and asking for a place says "Sorry, I didn't understand". "Hey Google, navigate to … with Horizon Nav" is handled (German home/work words and favourites too, and a single clear match starts straight away). Without the app named, Horizon Nav must be the default navigation app in the Android Auto settings. Needs testing in the car.

## Can't be fixed in the app

- **The app not showing up in Android Auto on some phones**, especially GrapheneOS and LineageOS, or without installing through AAEnabler. Android Auto only lists unofficial apps under certain conditions; the app shows a note about AAEnabler.
- **Some speed limits are still wrong.** Several sources now vote on the limit (HERE, TomTom, Mapbox, OpenStreetMap, Mapillary road signs, Autobahn roadworks and drivers' own reports), and a wrong value can be reported by tapping the sign or under Melden in the car. Where every source is wrong, it can also be corrected on openstreetmap.org.
- **Waze-style live police alerts.** Reports (police, crash, hazard, jam…) are now shared with other Horizon Nav drivers through the reports server, but they only help where enough people use the app.

## Done (from these requests)

- Map options on Android Auto (#12): a layers button with switches for 3D, traffic, satellite, cameras, reports, car parks, rain radar and route-only traffic.
- Share your arrival time as a live link (map, position, arrival time, distance left), from the phone or the car.
- Live car park occupancy: on arrival, in the parking search and on the map.
- Automatic rerouting onto a faster route (with a set minimum saving, an announcement and "Alte Route" to go back).
- Motorhome / caravan: pitches, water & waste, LPG in the quick search, and low-bridge height warnings.
- DWD severe weather warnings on the route ahead (Germany), and the rescue lane reminder in motorway jams.
- Speed limits from several sources at once, with a dispute dot, details on tap, and drivers' own limit reports.
- Search puts the town ("Berlin") before nearby streets ("Berliner Straße"); results in German, "offline" only without signal.

- Custom default zoom, with reset.
- Traffic overview when stopped (with a setting).
- Remember where you parked (phone, after Android Auto).
- Send a destination from your phone to the head unit.
- Search tip to add a Mapbox key when the free servers are busy.
- Trees missing on Android Auto (GitHub #19): the car map is now drawn at close to the phone's zoom.
- Wrong road name under the arrow (GitHub #20).
- AAEnabler not finding the update file (GitHub #19): updates are now saved to Downloads for AAEnabler installs.
- The in-app update download stalling at full size.
- Turn-by-turn voice silent for one user.
- Updating no longer takes the app out of Android Auto for AAEnabler installs: the new version is saved to Downloads to install with AAEnabler.
- Missing 3D buildings: the "keep the road ahead clear" area no longer cuts through buildings. There's no straight line ahead without a route, and the strip along a route is narrower.
- The old "Search & keys" name on the offline maps page.
- Crash on start on a Teyes CC3 2K (Android 10).
- Black screen on Android Auto.
- Satellite view not working.
- Android Auto support, and the Android Auto layout fixes: the trip card no longer covers the arrow, the speed sign sits in the corner, the card is sized for wide screens, and the map page matches the app.
- 2D / 3D on Android Auto, and the map remembers 2D or 3D between drives (GitHub #17).
- Car types for the arrow (saloon, hatchback, estate, SUV, pickup, van, sports), including the real 3D model.
- Your own picture as the locator.
- Bigger / smaller speedometer and speed limit sign.
- Road name size.
- Voice without road names.
- "Exit" rather than "junction" outside the UK.
- Stop signs on the map with a chime.
- km/h and kilometres.
- Colour for the other route lines.
- Light traffic shown in green (optional, main roads only).
- E85 / bioethanol.
- Live fuel prices in France, Austria and Germany.
- Avoid tolls only in chosen countries.
- Electric car planning: battery, charge level at arrival, and charging stops along the route.
- Easier stops: one-tap fuel, coffee and so on along the route.
- Change Home and Work, and rename favourites.
- Report police, crashes and hazards without a route.
- The Saved & recent list can be folded away or swiped off.
- Offline maps for many more countries, and several areas at once.
- Mapbox key working on a second device (stray spaces in pasted keys are removed).
- The fuel and parking search errors (HTTP 429 / 502) fixed.
- Adding a stop no longer hangs.
- A donate link.
- Change the app's language (#2): German or English, picked in Settings > Sound & voice (Android 13+); German on German devices, English on all others. Settings, setup and the What's new card are translated.
- Every location of a shop or chain ("Alle Standorte"), with drive times, logos, sorting by quickest / nearest / on my route, and a list on Android Auto.
- Logos and opening times in search results; a card for a place or spot tapped on the phone's map (Go / Add as stop).
- Own design (route, arrow and accent colour, lighter or darker map) with a live preview; waving 3D flags with a chequered finish flag.
- Tolls and vignettes on the route cards; a route picked by hand is kept through reroutes and on Android Auto.
- Roundabouts the right way round on the turn card, the map and the head-up display; exact exit angle on TomTom routes.
- Tags for favourites, and rest areas offered with the two-hour break reminder.
