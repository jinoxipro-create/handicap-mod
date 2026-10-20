# Défi Handicaps (mod Fabric client, Minecraft 1.21.11)

Toutes les 10 minutes, un nouveau **palier** démarre :

- palier 1 = 1 handicap, palier 2 = 2 handicaps, palier 3 = 3 handicaps, etc. ;
- les handicaps d'un palier sont **différents** de ceux du palier précédent (pas d'accumulation : les anciens sont retirés) ;
- les handicaps actifs (et le chrono) s'affichent **en haut à gauche** de l'écran.

Le mod est **client uniquement** : il se met seulement chez toi (solo ou serveur), rien à installer côté serveur.
Les handicaps sont des **contraintes de jeu fictives**, pas des représentations fidèles de ces handicaps.

## Installation (si tu as déjà le .jar)
Dossier `mods` de Minecraft 1.21.11 avec Fabric Loader + Fabric API 0.141.x pour 1.21.11 + le .jar du mod.

## Fabriquer le .jar
Option A (sur ton PC, Java 21 ou plus) : dans ce dossier, `./gradlew build` (Windows : `gradlew.bat build`).
Le .jar à utiliser est dans `build/libs/` : celui SANS `-sources` dans le nom.

Option B (sans rien installer) : mets ce dossier sur un dépôt GitHub. L'onglet Actions compile
automatiquement et propose le .jar en téléchargement (artefact « Artifacts »).

## Commandes de test (tapées dans le chat du jeu)
- `/handicap list` : liste tous les handicaps avec leur identifiant
- `/handicap test <id>` : active UN SEUL handicap pour le tester (chrono en pause)
- `/handicap round <n>` : saute directement au palier n
- `/handicap skip` : passe au palier suivant tout de suite
- `/handicap interval <minutes>` : change la durée d'un palier (10 par défaut)
- `/handicap status` : affiche le palier et le temps restant
- `/handicap reset` : remet tout à zéro (aucun handicap, chrono relancé)

Le défi redémarre à zéro à chaque connexion à un monde ou à un serveur.

## Les 18 handicaps
Cécité, Vision en tunnel, Hémianopsie, Scotome, Achromatopsie (approximation), Sensibilité à la lumière
(éblouissements doux, jamais de flash rapide), Surdité, Perte auditive partielle, Trouble de la parole
(30 s entre deux messages), Aphasie (phrases prédéfinies), Tremblements essentiels, Tics moteurs,
Mobilité réduite, Amputation d'une jambe, Main secondaire indisponible, Temps de réaction allongé,
Faiblesse musculaire (jauge d'endurance), TDAH (fausses notifications).

Volontairement exclus : l'épilepsie (risque réel pour les spectateurs photosensibles) et les
troubles psychiques, que ta liste conseillait déjà d'éviter de représenter comme dangereux.

## Si le volume reste bas après un crash
Les effets de surdité baissent le volume général du jeu. Il est rendu quand l'effet s'arrête ou
que tu quittes proprement ; après un crash, remets simplement le volume dans Options > Musique et sons.
