; ----------------------------------------
; Name : Draw Nodal network
; Date : (C)2025
; Site : https://github.com/BlackCreepyCat
; ----------------------------------------

Graphics 800,600,0,2
SetBuffer BackBuffer()

; Définition du type NodalNet
Type NodalNet
    Field NType%        ; 0 = Point de liaison, 1 = Valeur
    Field Radius%       ; Rayon du cercle pour les nodes de valeur
    Field Parent.NodalNet ; Node parent
    Field Value#        ; Valeur du node
    Field Px%          ; Position X
    Field Py%          ; Position Y
    Field Caption$     ; Légende du node
End Type

; Variables globales pour la caméra
Global cam_zoom# = 1.0
Global cam_x# = 0
Global cam_y# = 0
Global mouse_down = False
Global last_mx%, last_my%
Global selected_node.NodalNet = Null ; Node actuellement sélectionné
Global last_selected.NodalNet = Null ; Dernier node sélectionné

; Création du réseau de test
Function CreateTestNetwork()
    ; Node racine (point de liaison)
    root.NodalNet = New NodalNet
    root\NType = 0
    root\Px = 00
    root\Py = 00
    root\Caption = "Root"
    
    ; Création de nodes de valeur mieux répartis
    For i = 1 To 15
        node.NodalNet = New NodalNet
        node\NType = 1
        node\Value = Rnd(10,100)
        node\Radius = node\Value / 2
        node\Parent = root
        node\Caption = "Node" + i
        ; Répartition en spirale pour éviter les chevauchements
        angle# = i * 24 ; Angle plus petit pour plus de points
        dist# = 50 + i * 20 ; Distance croissante
        node\Px = root\Px + Cos(angle) * dist
        node\Py = root\Py + Sin(angle) * dist
    Next
End Function

; Fonction pour vérifier si la souris est sur un node
Function CheckNodeUnderMouse.NodalNet()
    mx# = (MouseX() - 400) / cam_zoom - cam_x
    my# = (MouseY() - 300) / cam_zoom - cam_y
    
    For node.NodalNet = Each NodalNet
        If node\NType = 1 Then ; Seulement les nodes de valeur
            dx# = node\Px - mx
            dy# = node\Py - my
            dist# = Sqr(dx*dx + dy*dy)
            If dist < node\Radius Then
                Return node
            EndIf
        EndIf
    Next
    Return Null
End Function

; Fonction principale
CreateTestNetwork()

While Not KeyHit(1) ; ESC pour quitter
    Cls
    
    ; Gestion du zoom avec la molette
    zoom_change = MouseZSpeed()
    If zoom_change > 0 Then cam_zoom = cam_zoom * 1.1
    If zoom_change < 0 Then cam_zoom = cam_zoom / 1.1
    If cam_zoom < 0.1 Then cam_zoom = 0.1
    
    ; Gestion de la souris - Clic gauche
    If MouseDown(1) Then
        If Not mouse_down Then
            mouse_down = True
            last_mx = MouseX()
            last_my = MouseY()
            
            ; Vérifier si on clique sur un node
            selected_node = CheckNodeUnderMouse()
            If selected_node <> Null Then
                last_selected = selected_node ; Mettre à jour le dernier sélectionné
            EndIf
        Else
            If selected_node <> Null Then
                ; Déplacer le node sélectionné
                mx# = (MouseX() - 400) / cam_zoom - cam_x
                my# = (MouseY() - 300) / cam_zoom - cam_y
                selected_node\Px = mx
                selected_node\Py = my
            Else
                ; Déplacer la caméra
                cam_x = cam_x + (MouseX() - last_mx) / cam_zoom
                cam_y = cam_y + (MouseY() - last_my) / cam_zoom
                last_mx = MouseX()
                last_my = MouseY()
            EndIf
        EndIf
    Else
        mouse_down = False
        selected_node = Null
    EndIf
    
; Gestion du clic droit - Ajout d'un nouveau node
If MouseHit(2) And last_selected <> Null Then
    new_node.NodalNet = New NodalNet
    new_node\NType = 1
    new_node\Value = Rnd(10,100)
    new_node\Radius = new_node\Value / 2
    new_node\Parent = last_selected
    new_node\Caption = "New" + Rnd(1,1000)
    ; Positionner le nouveau node à la position de la souris dans le monde
    new_node\Px = (MouseX() - 400) / cam_zoom - cam_x
    new_node\Py = (MouseY() - 300) / cam_zoom - cam_y
EndIf
    
    ; Dessin du réseau
    For node.NodalNet = Each NodalNet
        screen_x# = (node\Px + cam_x) * cam_zoom + 400
        screen_y# = (node\Py + cam_y) * cam_zoom + 300
        
        ; Dessin des lignes vers le parent
        If node\Parent <> Null Then
            parent_x# = (node\Parent\Px + cam_x) * cam_zoom + 400
            parent_y# = (node\Parent\Py + cam_y) * cam_zoom + 300
            Color 100,100,100
            Line screen_x, screen_y, parent_x, parent_y
        EndIf
        
        ; Dessin des nodes
        If node\NType = 0 Then
            Color 255,0,0
            Plot screen_x, screen_y
            Oval screen_x-3, screen_y-3, 6, 6, 0
            Color 255,255,255
            Text screen_x, screen_y+10, node\Caption, True, False
        Else
            radius_scaled# = node\Radius * cam_zoom
            If node = selected_node Then
                Color 0,255,0 ; Vert si actuellement sélectionné
            ElseIf node = last_selected Then
                Color 0,255,0 ; Vert si dernier sélectionné
            Else
                Color 0,0,255 ; Bleu sinon
            EndIf
            Oval screen_x - radius_scaled, screen_y - radius_scaled, radius_scaled * 2, radius_scaled * 2, 0
            Color 255,255,255
            Text screen_x, screen_y-10, node\Caption, True, False ; Caption au-dessus
            Text screen_x, screen_y, Int(node\Value), True, True ; Valeur au centre
        EndIf
    Next
    
    ; Affichage des infos
    Color 255,255,255
    Text 10,10, "Zoom: "+cam_zoom
    Text 10,25, "Molette: zoom | Gauche: déplacer | Droit: ajouter node"
    
    Flip
Wend

End